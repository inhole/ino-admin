# Admin Web Instructions

- Keep API access in `src/api`; components must render explicit loading, empty, and error states.
- Preserve installed shadcn UI primitives without local edits. Compose them as-is, and create application-specific wrapper or custom components when behavior or styling must differ. Add missing shadcn components through the CLI only when existing primitives cannot cover the need.
- Use semantic HTML, visible keyboard focus, labelled controls, and sufficient color contrast.
- Do not treat hidden routes or buttons as authorization; the server remains authoritative.
- Run lint, typecheck, unit tests, and build for UI behavior changes.
