#!/usr/bin/env python3
"""Drive plant order through reservation, compensation, scrap, and overlap."""

import base64
import json
import sys
import time
import urllib.error
import urllib.request
import uuid
from concurrent.futures import ThreadPoolExecutor

PORTS = {
    "item": 8221,
    "bom": 8222,
    "order": 8223,
    "explosion": 8224,
    "material": 8225,
    "release": 8226,
    "station": 8227,
    "quality": 8228,
    "scrap": 8229,
    "receipt": 8230,
    "labor": 8231,
    "cost": 8232,
    "audit": 8233,
}
MGMT = "http://127.0.0.1:15684"
AUTH = "platform:platform"
failures = []


def check(name, ok, detail=""):
    if ok:
        print(f"PASS  {name}", flush=True)
    else:
        print(f"FAIL  {name} {detail}", flush=True)
        failures.append(f"{name}: {detail}")


def call(method, url, body=None, auth=None, timeout=20):
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("content-type", "application/json")
    if auth:
        request.add_header("Authorization", "Basic " + base64.b64encode(auth.encode()).decode())
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            raw = response.read().decode()
            return response.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as ex:
        raw = ex.read().decode()
        try:
            parsed = json.loads(raw) if raw else None
        except json.JSONDecodeError:
            parsed = raw
        return ex.code, parsed
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError):
        return 0, None


def wait_ok(url, seconds=300, auth=None):
    deadline = time.time() + seconds
    while time.time() < deadline:
        if call("GET", url, auth=auth)[0] == 200:
            return True
        time.sleep(1)
    return False


def until(label, fn, seconds=90):
    deadline = time.time() + seconds
    last = None
    while time.time() < deadline:
        last = fn()
        if last is not None:
            return last
        time.sleep(0.4)
    check(label, False, f"timed out last={last!r}")
    return None


def depth(name):
    status, body = call("GET", f"{MGMT}/api/queues/%2F/{name}", auth=AUTH)
    if status != 200 or not isinstance(body, dict):
        return None
    return body.get("messages", 0)


def grew(name, before, amount=1):
    def ready():
        now = depth(name)
        if now is not None and now >= before + amount:
            return now
        return None
    return until(f"dlq {name}", ready, 40)


def base(name):
    return f"http://127.0.0.1:{PORTS[name]}"


def directory_ready(service, sku, kind):
    def seen():
        code, row = call("GET", f"{base(service)}/directory/{sku}")
        if code == 200 and isinstance(row, dict) and row.get("kind") == kind:
            return row
        return None
    return until(f"{service} directory {sku}", seen, 40) is not None


def item(sku, kind, standard, labor):
    status, body = call("POST", f"{base('item')}/items", {
        "sku": sku, "kind": kind, "standardCents": standard, "laborCents": labor,
    })
    check(f"item {sku}", status == 201, body)
    if status != 201:
        return False
    check(f"bom directory {sku}", directory_ready("bom", sku, kind))
    check(f"order directory {sku}", directory_ready("order", sku, kind))
    return True


def bom(parent, component, qty):
    status, body = call("POST", f"{base('bom')}/boms", {
        "parentSku": parent, "componentSku": component, "qtyPer": qty,
    })
    check(f"bom {parent} {component}", status == 201, body)
    return status == 201


def wait_boms(parent, count):
    def seen():
        code, body = call("GET", f"{base('order')}/boms/{parent}")
        if code == 200 and isinstance(body, list) and len(body) >= count:
            return body
        return None
    return until(f"boms {parent}", seen, 40)


def put_stock(sku, on_hand):
    status, body = call("POST", f"{base('material')}/stock", {"sku": sku, "onHand": on_hand})
    check(f"stock {sku}", status == 201, body)
    return status == 201


def on_hand(sku):
    code, body = call("GET", f"{base('material')}/stock/{sku}")
    if code != 200 or not isinstance(body, dict):
        return None
    return body.get("onHand")


def place(item_sku, quantity, station="STATION-1", lot="LOT-A", dock="DOCK-1", request_id=None):
    return call("POST", f"{base('order')}/orders", {
        "requestId": request_id or str(uuid.uuid4()),
        "itemSku": item_sku,
        "quantity": quantity,
        "stationToken": station,
        "lot": lot,
        "dock": dock,
    })


def order(oid):
    code, body = call("GET", f"{base('order')}/orders/{oid}")
    return body if code == 200 and isinstance(body, dict) else None


def audit_has(oid, status):
    code, body = call("GET", f"{base('audit')}/audits/{oid}")
    if code != 200 or not isinstance(body, list):
        return False
    return any(item.get("kind") == "OrderFact" and item.get("status") == status for item in body)


def assemble(labor, parts):
    token = uuid.uuid4().hex[:8]
    parent = f"FG-{token}"
    if not item(parent, "FINISHED", 0, labor):
        return None
    skus = {}
    for part in parts:
        sku = f"{part['name']}-{token}"
        skus[part["name"]] = sku
        if not item(sku, "COMPONENT", part["standard"], 0):
            return None
    for part in parts:
        if not bom(parent, skus[part["name"]], part["qty"]):
            return None
    if parts and wait_boms(parent, len(parts)) is None:
        return None
    for part in parts:
        if "on_hand" in part and not put_stock(skus[part["name"]], part["on_hand"]):
            return None
    return parent, skus


def stable(sku_amounts, seconds=12):
    deadline = time.time() + seconds
    while time.time() < deadline:
        for sku, expected in sku_amounts.items():
            if on_hand(sku) != expected:
                return False
        time.sleep(1)
    return True


def main():
    for name in PORTS:
        check(f"{name} health", wait_ok(f"{base(name)}/actuator/health"))
    check("rabbit health", wait_ok(f"{MGMT}/api/overview", auth=AUTH))

    status, _ = call("POST", f"{base('material')}/stock", {"sku": "NEG", "onHand": -1})
    check("negative on-hand is 400", status == 400, status)
    status, _ = call("POST", f"{base('item')}/items", {"sku": "BAD:PART", "kind": "COMPONENT", "standardCents": 1, "laborCents": 0})
    check("sku with colon is 400", status == 400, status)
    status, _ = call("POST", f"{base('item')}/items", {"sku": "BAD|PART", "kind": "COMPONENT", "standardCents": 1, "laborCents": 0})
    check("sku with bar is 400", status == 400, status)
    status, _ = place(" ", 0, station=" ", lot=" ", dock=" ", request_id=" ")
    check("blank order is 400", status == 400, status)

    dup = "DUP-" + uuid.uuid4().hex[:8]
    check("duplicate setup", item(dup, "COMPONENT", 10, 0))
    status, _ = call("POST", f"{base('item')}/items", {"sku": dup, "kind": "COMPONENT", "standardCents": 10, "laborCents": 0})
    check("duplicate item is 409", status == 409, status)

    component = "COMP-" + uuid.uuid4().hex[:8]
    check("component setup", item(component, "COMPONENT", 10, 0))
    status, _ = place(component, 1)
    check("component order is 400", status == 400, status)
    missing = "NO-" + uuid.uuid4().hex[:8]
    status, _ = place(missing, 1)
    check("unknown item is 404", status == 404, status)

    parent = "PAR-" + uuid.uuid4().hex[:8]
    child = "CH-" + uuid.uuid4().hex[:8]
    check("parent setup", item(parent, "FINISHED", 0, 10))
    status, _ = call("POST", f"{base('bom')}/boms", {"parentSku": "NO-PARENT", "componentSku": child, "qtyPer": 1})
    check("unknown bom parent is 404", status == 404, status)
    check("child setup", item(child, "COMPONENT", 10, 0))
    status, _ = call("POST", f"{base('bom')}/boms", {"parentSku": parent, "componentSku": "NO-COMP", "qtyPer": 1})
    check("unknown bom component is 404", status == 404, status)
    status, first = call("POST", f"{base('bom')}/boms", {"parentSku": parent, "componentSku": child, "qtyPer": 1})
    check("bom line accepted", status == 201, first)
    status, _ = call("POST", f"{base('bom')}/boms", {"parentSku": parent, "componentSku": child, "qtyPer": 1})
    check("duplicate bom line is 409", status == 409, status)

    built = assemble(100, [
        {"name": "BOLT", "standard": 20, "qty": 4, "on_hand": 20},
        {"name": "NUT", "standard": 30, "qty": 2, "on_hand": 20},
    ])
    check("happy bill", built is not None, built)
    if built:
        happy_parent, happy_skus = built
        request_id = str(uuid.uuid4())
        status, created = place(happy_parent, 2, request_id=request_id)
        check("order accepted", status == 201 and isinstance(created, dict), created)
        oid = created["id"] if isinstance(created, dict) else ""
        bolt = happy_skus["BOLT"]
        nut = happy_skus["NUT"]
        expected_lines = f"{bolt}:8:20|{nut}:4:30"

        def costed():
            body = order(oid)
            cost_code, cost = call("GET", f"{base('cost')}/costs/{oid}")
            station_code, station_row = call("GET", f"{base('station')}/stations/{oid}")
            quality_code, quality = call("GET", f"{base('quality')}/inspections/{oid}")
            receipt_code, receipt = call("GET", f"{base('receipt')}/receipts/{oid}")
            release_code, release = call("GET", f"{base('release')}/releases/{oid}")
            scrap_code, _scrap = call("GET", f"{base('scrap')}/scraps/{oid}")
            explosion_code, explosion = call("GET", f"{base('explosion')}/explosions/{oid}")
            labor_code, labor = call("GET", f"{base('labor')}/labor/{oid}")
            if not body or body.get("status") != "COSTED" or body.get("costStatus") != "POSTED":
                return None
            if body.get("materialStatus") != "CONSUMED" or body.get("materialCents") != 280:
                return None
            if body.get("laborCents") != 200 or body.get("costCents") != 480:
                return None
            if cost_code != 200 or not isinstance(cost, dict) or cost.get("amountCents") != 480:
                return None
            if cost.get("materialCents") != 280 or cost.get("laborCents") != 200:
                return None
            if on_hand(bolt) != 12 or on_hand(nut) != 16:
                return None
            if station_code != 200 or not isinstance(station_row, dict) or station_row.get("status") != "PASSED":
                return None
            if quality_code != 200 or not isinstance(quality, dict) or quality.get("status") != "ACCEPTED":
                return None
            if receipt_code != 200 or not isinstance(receipt, dict) or receipt.get("status") != "RECEIVED" or receipt.get("quantity") != 2:
                return None
            if release_code != 200 or not isinstance(release, dict) or release.get("status") != "RELEASED":
                return None
            if scrap_code != 404 or explosion_code != 200 or not isinstance(explosion, dict):
                return None
            if explosion.get("status") != "EXPLODED" or explosion.get("materialCents") != 280 or explosion.get("lines") != expected_lines:
                return None
            if labor_code != 200 or not isinstance(labor, dict) or labor.get("status") != "POSTED" or labor.get("amountCents") != 200:
                return None
            if not audit_has(oid, "COSTED"):
                return None
            return body

        check("two gears cost 480 and consume both bins", until("happy", costed, 120) is not None)
        status, replayed = place(happy_parent, 2, request_id=request_id)
        check("replay returns the same order", status == 200 and isinstance(replayed, dict) and replayed.get("id") == oid, replayed)
        check("replay does not reserve again", stable({bolt: 12, nut: 16}))

    empty_decoy = "IDLE-" + uuid.uuid4().hex[:8]
    empty_built = assemble(40, [])
    check("empty parent", empty_built is not None and item(empty_decoy, "COMPONENT", 5, 0) and put_stock(empty_decoy, 7))
    if empty_built:
        status, created = place(empty_built[0], 1)
        check("empty order accepted", status == 201 and isinstance(created, dict), created)
        empty_id = created["id"] if isinstance(created, dict) else ""

        def emptied():
            body = order(empty_id)
            explosion_code, _ = call("GET", f"{base('explosion')}/explosions/{empty_id}")
            if body and body.get("status") == "CANCELLED" and body.get("reason") == "EMPTY" and explosion_code == 404 and on_hand(empty_decoy) == 7:
                return body
            return None

        check("empty bill cancels before a reservation", until("empty", emptied, 60) is not None)

    overflow = assemble(10, [{"name": "HUGE", "standard": 1, "qty": 2147483647, "on_hand": 5}])
    check("overflow bill", overflow is not None)
    if overflow:
        huge_parent, huge_skus = overflow
        status, created = place(huge_parent, 2)
        check("overflow order accepted", status == 201 and isinstance(created, dict), created)
        huge_id = created["id"] if isinstance(created, dict) else ""

        def priced():
            body = order(huge_id)
            explosion_code, _ = call("GET", f"{base('explosion')}/explosions/{huge_id}")
            if body and body.get("status") == "CANCELLED" and body.get("reason") == "PRICE" and explosion_code == 404 and on_hand(huge_skus["HUGE"]) == 5:
                return body
            return None

        check("overflow cancels before a reservation", until("price", priced, 60) is not None)

    short = assemble(15, [
        {"name": "A", "standard": 10, "qty": 1, "on_hand": 10},
        {"name": "B", "standard": 10, "qty": 1, "on_hand": 0},
    ])
    check("short bill", short is not None)
    if short:
        short_parent, short_skus = short
        status, created = place(short_parent, 1)
        check("short order accepted", status == 201 and isinstance(created, dict), created)
        short_id = created["id"] if isinstance(created, dict) else ""

        def shortened():
            body = order(short_id)
            if body and body.get("status") == "CANCELLED" and body.get("reason") == "SHORT" and on_hand(short_skus["A"]) == 10 and on_hand(short_skus["B"]) == 0:
                return body
            return None

        check("short second component releases the first", until("short", shortened, 70) is not None)

    station_dlq = depth("plant.station.pass.dlq")
    broken = assemble(20, [{"name": "PIN", "standard": 12, "qty": 1, "on_hand": 8}])
    check("broken bill", broken is not None)
    if broken:
        broken_parent, broken_skus = broken
        status, created = place(broken_parent, 1, station="STATION-BROKEN")
        check("broken order accepted", status == 201 and isinstance(created, dict), created)
        broken_id = created["id"] if isinstance(created, dict) else ""

        def station_failed():
            body = order(broken_id)
            station_code, _ = call("GET", f"{base('station')}/stations/{broken_id}")
            release_code, release = call("GET", f"{base('release')}/releases/{broken_id}")
            if body and body.get("status") == "CANCELLED" and body.get("reason") == "STATION" and station_code == 404 and release_code == 200 and isinstance(release, dict) and release.get("status") == "VOID" and on_hand(broken_skus["PIN"]) == 8:
                return body
            return None

        check("broken station returns the component", until("station", station_failed, 80) is not None)
        check("broken station dead-letters the pass", grew("plant.station.pass.dlq", station_dlq) == (None if station_dlq is None else station_dlq + 1))

    held = assemble(20, [{"name": "CLIP", "standard": 9, "qty": 1, "on_hand": 6}])
    check("hold bill", held is not None)
    if held:
        held_parent, held_skus = held
        status, created = place(held_parent, 1, lot="LOT-HOLD")
        check("hold order accepted", status == 201 and isinstance(created, dict), created)
        held_id = created["id"] if isinstance(created, dict) else ""

        def held_lot():
            body = order(held_id)
            quality_code, _ = call("GET", f"{base('quality')}/inspections/{held_id}")
            receipt_code, _ = call("GET", f"{base('receipt')}/receipts/{held_id}")
            release_code, release = call("GET", f"{base('release')}/releases/{held_id}")
            if body and body.get("status") == "CANCELLED" and body.get("reason") == "HOLD" and quality_code == 404 and receipt_code == 404 and release_code == 200 and isinstance(release, dict) and release.get("status") == "VOID" and on_hand(held_skus["CLIP"]) == 6:
                return body
            return None

        check("held lot returns the component and receives nothing", until("hold", held_lot, 80) is not None)

    scrapped = assemble(40, [{"name": "WIRE", "standard": 50, "qty": 1, "on_hand": 9}])
    check("scrap bill", scrapped is not None)
    if scrapped:
        scrap_parent, scrap_skus = scrapped
        status, created = place(scrap_parent, 1, lot="LOT-SCRAP")
        check("scrap order accepted", status == 201 and isinstance(created, dict), created)
        scrap_id = created["id"] if isinstance(created, dict) else ""

        def scrapped_lot():
            body = order(scrap_id)
            scrap_code, scrap_row = call("GET", f"{base('scrap')}/scraps/{scrap_id}")
            receipt_code, _ = call("GET", f"{base('receipt')}/receipts/{scrap_id}")
            cost_code, cost = call("GET", f"{base('cost')}/costs/{scrap_id}")
            if not body or body.get("status") != "SCRAPPED" or body.get("reason") != "SCRAP":
                return None
            if body.get("costStatus") != "POSTED" or body.get("materialStatus") != "CONSUMED":
                return None
            if body.get("materialCents") != 50 or body.get("laborCents") != 40 or body.get("costCents") != 90:
                return None
            if on_hand(scrap_skus["WIRE"]) != 8 or receipt_code != 404:
                return None
            if scrap_code != 200 or not isinstance(scrap_row, dict) or scrap_row.get("quantity") != 1:
                return None
            if cost_code != 200 or not isinstance(cost, dict) or cost.get("amountCents") != 90:
                return None
            return body

        check("scrap keeps the material and still rolls the cost", until("scrap", scrapped_lot, 90) is not None)

    receive_dlq = depth("plant.receipt.receive.dlq")
    closed = assemble(25, [{"name": "CAP", "standard": 15, "qty": 1, "on_hand": 4}])
    check("closed bill", closed is not None)
    if closed:
        closed_parent, closed_skus = closed
        status, created = place(closed_parent, 1, dock="DOCK-CLOSED")
        check("closed order accepted", status == 201 and isinstance(created, dict), created)
        closed_id = created["id"] if isinstance(created, dict) else ""

        def dock_closed():
            body = order(closed_id)
            receipt_code, _ = call("GET", f"{base('receipt')}/receipts/{closed_id}")
            quality_code, quality = call("GET", f"{base('quality')}/inspections/{closed_id}")
            release_code, release = call("GET", f"{base('release')}/releases/{closed_id}")
            if body and body.get("status") == "CANCELLED" and body.get("reason") == "RECEIPT" and receipt_code == 404 and quality_code == 200 and isinstance(quality, dict) and quality.get("status") == "VOID" and release_code == 200 and isinstance(release, dict) and release.get("status") == "VOID" and on_hand(closed_skus["CAP"]) == 4:
                return body
            return None

        check("closed dock voids the inspection and returns the component", until("receipt", dock_closed, 90) is not None)
        check("closed dock dead-letters the receipt", grew("plant.receipt.receive.dlq", receive_dlq) == (None if receive_dlq is None else receive_dlq + 1))

    scarce = assemble(30, [{"name": "WIDGET", "standard": 11, "qty": 1, "on_hand": 5}])
    check("scarce bill", scarce is not None)
    if scarce:
        scarce_parent, scarce_skus = scarce

        def one_scarce(_):
            return place(scarce_parent, 1)

        with ThreadPoolExecutor(max_workers=8) as pool:
            raced = list(pool.map(one_scarce, range(8)))
        ids = [body["id"] for code, body in raced if code == 201 and isinstance(body, dict)]
        check("eight scarce orders accepted", len(ids) == 8, [code for code, _body in raced])

        def scarce_settled():
            states = []
            for oid in ids:
                body = order(oid)
                if not body or body.get("status") not in ("COSTED", "CANCELLED"):
                    return None
                if body.get("status") == "COSTED" and (body.get("materialStatus") != "CONSUMED" or body.get("costStatus") != "POSTED"):
                    return None
                states.append(body)
            winners = [row for row in states if row.get("status") == "COSTED"]
            losers = [row for row in states if row.get("reason") == "SHORT"]
            if len(winners) != 5 or len(losers) != 3 or on_hand(scarce_skus["WIDGET"]) != 0:
                return None
            return states

        check("five of eight orders fit the bin", until("scarce", scarce_settled, 150) is not None)

    pair = assemble(30, [
        {"name": "PLATE", "standard": 20, "qty": 1, "on_hand": 100},
        {"name": "RIVET", "standard": 5, "qty": 1, "on_hand": 1},
    ])
    check("rivet bill", pair is not None)
    if pair:
        pair_parent, pair_skus = pair

        def one_pair(_):
            return place(pair_parent, 1)

        with ThreadPoolExecutor(max_workers=2) as pool:
            raced = list(pool.map(one_pair, range(2)))
        ids = [body["id"] for code, body in raced if code == 201 and isinstance(body, dict)]
        check("two rivet orders accepted", len(ids) == 2, [code for code, _body in raced])

        def rivet_settled():
            states = []
            for oid in ids:
                body = order(oid)
                if not body or body.get("status") not in ("COSTED", "CANCELLED"):
                    return None
                if body.get("status") == "COSTED" and body.get("materialStatus") != "CONSUMED":
                    return None
                states.append(body)
            winners = [row for row in states if row.get("status") == "COSTED"]
            losers = [row for row in states if row.get("reason") == "SHORT"]
            if len(winners) != 1 or len(losers) != 1:
                return None
            if on_hand(pair_skus["PLATE"]) != 99 or on_hand(pair_skus["RIVET"]) != 0:
                return None
            return states

        check("one rivet completes and the loser releases the plate", until("rivet", rivet_settled, 120) is not None)

    print("\n== summary ==")
    if failures:
        print(f"{len(failures)} failed")
        for failure in failures:
            print(f" - {failure}")
        sys.exit(1)
    print("plant order smoke passed")


if __name__ == "__main__":
    main()
