ALTER TABLE agent_registration
    ADD COLUMN secret_encryption_public_key TEXT NOT NULL DEFAULT '';
