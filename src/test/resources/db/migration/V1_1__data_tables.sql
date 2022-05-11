CREATE TABLE applications (
  id           TEXT PRIMARY KEY,
  artifact_id   TEXT
);

CREATE TABLE application_configs (
  id TEXT PRIMARY KEY,
  application_id TEXT,
  data JSONB
);

CREATE TABLE clients (
  client_id TEXT PRIMARY KEY,
  application_config_id TEXT,
  auto_upgrade BOOLEAN
);

CREATE TABLE client_heartbeat_data (
  client_id TEXT PRIMARY KEY,
  data JSONB
);

CREATE TABLE client_environments (
  client_id TEXT PRIMARY KEY,
  data JSONB
);
