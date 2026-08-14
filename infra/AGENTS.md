# Infrastructure Instructions

- Keep local defaults non-sensitive and document every exposed host port.
- Never embed production credentials or destructive volume-reset behavior in normal startup commands.
- Add health checks for service readiness and validate Compose changes with `docker compose config`.
