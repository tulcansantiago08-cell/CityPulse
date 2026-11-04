#!/usr/bin/env python3
"""Carga el catálogo de ejemplo (data/firestore/events_seed.json) en la colección `events` de Firestore.

Usa la API REST con la clave de app/google-services.json, por lo que solo funciona mientras las
reglas de Firestore permitan escribir (modo de prueba). Después hay que publicar firestore.rules.

También borra de la colección los documentos que ya no están en el archivo (por ejemplo, eventos
de ejemplo que se reemplazaron por lugares reales), para que la app no los siga mostrando.

Uso:  python3 scripts/seed_firestore.py
"""
import json
import pathlib
import sys
import urllib.error
import urllib.parse
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parent.parent


def to_value(value):
    if value is None:
        return {"nullValue": None}
    if isinstance(value, bool):
        return {"booleanValue": value}
    if isinstance(value, int):
        return {"integerValue": str(value)}
    if isinstance(value, float):
        return {"doubleValue": value}
    if isinstance(value, list):
        return {"arrayValue": {"values": [to_value(v) for v in value]}}
    return {"stringValue": str(value)}


def main():
    config = json.loads((ROOT / "app" / "google-services.json").read_text())
    project = config["project_info"]["project_id"]
    key = config["client"][0]["api_key"][0]["current_key"]
    events = json.loads((ROOT / "data" / "firestore" / "events_seed.json").read_text())["events"]

    base = f"https://firestore.googleapis.com/v1/projects/{project}/databases/(default)/documents/events"
    for event in events:
        doc_id = event["id"]
        fields = {k: to_value(v) for k, v in event.items() if k != "id"}
        request = urllib.request.Request(
            f"{base}/{urllib.parse.quote(doc_id)}?key={key}",
            data=json.dumps({"fields": fields}).encode(),
            headers={"Content-Type": "application/json"},
            method="PATCH",
        )
        try:
            urllib.request.urlopen(request).read()
            print(f"ok  {doc_id}")
        except urllib.error.HTTPError as error:
            print(f"ERROR {doc_id}: {error.code} {error.read().decode()[:200]}", file=sys.stderr)
            sys.exit(1)
    print(f"{len(events)} eventos cargados en {project}")

    # Quita los documentos que ya no están en el archivo.
    keep = {event["id"] for event in events}
    removed = 0
    for doc_id in list_document_ids(base, key):
        if doc_id in keep:
            continue
        request = urllib.request.Request(f"{base}/{urllib.parse.quote(doc_id)}?key={key}", method="DELETE")
        try:
            urllib.request.urlopen(request).read()
            print(f"borrado  {doc_id}")
            removed += 1
        except urllib.error.HTTPError as error:
            print(f"ERROR al borrar {doc_id}: {error.code} {error.read().decode()[:200]}", file=sys.stderr)
            sys.exit(1)
    print(f"{removed} eventos antiguos borrados")


def list_document_ids(base, key):
    """Ids de todos los documentos de la colección (la API los devuelve por páginas)."""
    ids, token = [], None
    while True:
        query = {"key": key, "pageSize": 300, "mask.fieldPaths": "city"}
        if token:
            query["pageToken"] = token
        page = json.loads(urllib.request.urlopen(f"{base}?{urllib.parse.urlencode(query)}").read())
        ids += [doc["name"].rsplit("/", 1)[-1] for doc in page.get("documents", [])]
        token = page.get("nextPageToken")
        if not token:
            return ids


if __name__ == "__main__":
    main()
