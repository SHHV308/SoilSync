# Regional agronomy knowledge documents

Add reviewed Markdown documents using this layout:

```text
knowledge/{iso-3166-1-alpha-2-country-code}/{crop-slug}.md
knowledge/global/{crop-slug}.md
```

Country-specific documents take precedence over a global document for the same
crop. The advisor uses a document only when its front matter has `status:
vetted`, every metadata field below is complete, the source URL uses HTTPS, the
publication year and review date are valid, and the document has non-empty
content. Placeholder and incomplete documents are never used as advice. A
country-specific document must cover the selected country or administrative
region; use `National` for a source that applies across the country. For a
global document, use `Global` or `Worldwide`. Separate multiple covered
regions with `|`, for example `Nairobi County|Kiambu County`.

Required front matter:

```yaml
---
status: placeholder
source_name: "TODO: publication or package-of-practices title"
source_url: "TODO: official HTTPS source URL"
publisher: "TODO: publishing ministry, extension service, university, or institute"
publication_year: TODO
region_covered: "TODO: explicitly covered region"
last_reviewed: TODO
language: en
machine_translated: false
---
```

Replace `placeholder` with `vetted` only after a qualified human has checked
the source, region, crop, translation, and recommendations. Suggested sources
to investigate include FAO publications, national agriculture ministries and
extension services, university extension programs (including land-grant
extensions in the US), ICAR/KVK publications in India, and regional research
institutes. These are source suggestions, not evidence that SoilSync currently
has guidance from them. Mark machine-translated text `machine_translated: true`
and keep it unvetted until a qualified reviewer approves it.

No country/crop documents are currently vetted. The Köppen–Geiger climate-zone
field remains `UNCLASSIFIED` until a reviewed dataset and its license are
selected and integrated; latitude is not used as a climate classification.
