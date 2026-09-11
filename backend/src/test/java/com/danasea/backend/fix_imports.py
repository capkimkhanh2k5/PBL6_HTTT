import os

replacements = {
    "import com.danasea.backend.security.authentication.domain.exception.AccessDeniedException;": "import com.danasea.backend.security.authorization.domain.exception.AccessDeniedException;",
    "import com.danasea.backend.modules.account.infrastructure.persistence.repositories.JpaAuditLogRepository;": "import com.danasea.backend.modules.audit.infrastructure.persistence.repositories.JpaAuditLogRepository;",
    "import com.danasea.backend.modules.account.domain.models.AuditLog;": "import com.danasea.backend.modules.audit.domain.models.AuditLog;",
    "import com.danasea.backend.modules.account.infrastructure.persistence.entities.AuditLogJpaEntity;": "import com.danasea.backend.modules.audit.infrastructure.persistence.entities.AuditLogJpaEntity;"
}

for root, dirs, files in os.walk("."):
    for file in files:
        if file.endswith(".java"):
            path = os.path.join(root, file)
            with open(path, "r") as f:
                content = f.read()
            changed = False
            for old, new in replacements.items():
                if old in content:
                    content = content.replace(old, new)
                    changed = True
            if changed:
                with open(path, "w") as f:
                    f.write(content)
                print(f"Fixed {path}")
