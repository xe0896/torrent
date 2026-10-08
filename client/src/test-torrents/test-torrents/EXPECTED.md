# Test torrents — expected values

| File | Info hash | Total bytes | Pieces | Piece length | Last piece |
|---|---|---|---|---|---|
| `single.torrent` | `a58513c711c90af55fe70f18221dbe95abaeeb83` | 50000 | 4 | 16384 | 848 |
| `multi.torrent`  | `bb3f2ef9513ae0270e04cd174bac39dbcda94492` | 50005 | 4 | 16384 | 853 |

## single.torrent
- Top-level keys: `announce`, `comment`, `created by`, `creation date`, `info`
- `info`: `name` = `hello.bin`, `length` = 50000
- Content file: `hello.bin` (hash each 16384-byte piece and compare with `pieces`)

## multi.torrent
- Top-level keys: `announce`, `announce-list` (2 tiers), `info`
- `info.name` = `testdir`
- Files (in order):
  1. `a.txt` — 20000 bytes
  2. `docs/b.txt` — 5 bytes
  3. `docs/empty.txt` — 0 bytes
  4. `c.bin` — 30000 bytes
- Piece 1 (bytes 16384–32767) spans the end of `a.txt`, all of `b.txt`, the empty file, and the start of `c.bin`.
- Content folder: `testdir/`

## tiny.bencode
`d3:cow3:moo4:listl4:spami42ee4:spam4:eggse`
→ `{cow: "moo", list: ["spam", 42], spam: "eggs"}`
