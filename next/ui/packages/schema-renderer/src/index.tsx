import { FieldLabel } from "@wepush-next/ui";

export interface JsonSchema {
  $ref?: string;
  $defs?: Record<string, JsonSchema>;
  title?: string;
  description?: string;
  type?: "object" | "string" | "boolean" | "integer" | "number" | "array";
  format?: string;
  default?: unknown;
  enum?: unknown[];
  properties?: Record<string, JsonSchema>;
  required?: string[];
  additionalProperties?: boolean | JsonSchema;
  oneOf?: JsonSchema[];
  [key: string]: unknown;
}

export interface SchemaFormProps {
  schema: JsonSchema;
  value: Record<string, unknown>;
  onChange: (value: Record<string, unknown>) => void;
  disabled?: boolean;
}

export function SchemaForm({ schema, value, onChange, disabled = false }: SchemaFormProps) {
  const properties = Object.entries(schema.properties ?? {});
  const required = new Set(schema.required ?? []);

  return (
    <div className="schema-form">
      {properties.map(([name, field]) => (
        <SchemaField
          key={name}
          name={name}
          schema={resolveSchema(field, schema)}
          required={required.has(name)}
          value={value[name]}
          disabled={disabled}
          onChange={(nextValue) => onChange({ ...value, [name]: nextValue })}
        />
      ))}
    </div>
  );
}

interface SchemaFieldProps {
  name: string;
  schema: JsonSchema;
  value: unknown;
  required: boolean;
  disabled: boolean;
  onChange: (value: unknown) => void;
}

function SchemaField({ name, schema, value, required, disabled, onChange }: SchemaFieldProps) {
  const label = humanize(name);
  const id = `schema-field-${name}`;

  if (schema.type === "boolean") {
    return (
      <label className="schema-switch" htmlFor={id}>
        <span>
          <strong>{label}</strong>
          {schema.description ? <small>{schema.description}</small> : null}
        </span>
        <input
          id={id}
          type="checkbox"
          checked={Boolean(value ?? schema.default)}
          disabled={disabled}
          onChange={(event) => onChange(event.currentTarget.checked)}
        />
      </label>
    );
  }

  if (schema.enum) {
    return (
      <div className="schema-field">
        <FieldLabel>{label}{required ? " *" : ""}</FieldLabel>
        <select
          id={id}
          value={String(value ?? schema.default ?? "")}
          disabled={disabled}
          onChange={(event) => onChange(event.currentTarget.value)}
        >
          <option value="">请选择</option>
          {schema.enum.map((entry) => <option key={String(entry)} value={String(entry)}>{String(entry)}</option>)}
        </select>
      </div>
    );
  }

  if (schema.type === "object" || schema.oneOf || typeof schema.additionalProperties === "object") {
    return (
      <div className="schema-field">
        <FieldLabel hint="JSON 对象">{label}{required ? " *" : ""}</FieldLabel>
        <textarea
          id={id}
          rows={4}
          spellCheck={false}
          value={toJson(value ?? schema.default ?? {})}
          disabled={disabled}
          onChange={(event) => onChange(parseJsonOrText(event.currentTarget.value))}
        />
        {schema.description ? <small className="schema-field__description">{schema.description}</small> : null}
      </div>
    );
  }

  const numeric = schema.type === "integer" || schema.type === "number";
  return (
    <div className="schema-field">
      <FieldLabel hint={schema.format}>{label}{required ? " *" : ""}</FieldLabel>
      <input
        id={id}
        type={numeric ? "number" : schema.format === "password" ? "password" : "text"}
        value={String(value ?? schema.default ?? "")}
        disabled={disabled}
        required={required}
        onChange={(event) => onChange(numeric ? event.currentTarget.valueAsNumber : event.currentTarget.value)}
      />
      {schema.description ? <small className="schema-field__description">{schema.description}</small> : null}
    </div>
  );
}

export function defaultsForSchema(schema: JsonSchema): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(schema.properties ?? {}).flatMap(([name, field]) => {
      const value = defaultForSchema(resolveSchema(field, schema), schema);
      return value === undefined ? [] : [[name, value]];
    }),
  );
}

function resolveSchema(schema: JsonSchema, root: JsonSchema): JsonSchema {
  if (!schema.$ref?.startsWith("#/$defs/")) return schema;
  const name = schema.$ref.slice("#/$defs/".length);
  return root.$defs?.[name] ?? schema;
}

function defaultForSchema(schema: JsonSchema, root: JsonSchema): unknown {
  if (schema.default !== undefined) return structuredClone(schema.default);
  if (schema.type !== "object" || !schema.properties) return undefined;
  const nested = Object.fromEntries(
    Object.entries(schema.properties).flatMap(([name, field]) => {
      const value = defaultForSchema(resolveSchema(field, root), root);
      return value === undefined ? [] : [[name, value]];
    }),
  );
  return Object.keys(nested).length ? nested : undefined;
}

function humanize(value: string): string {
  return value.replace(/([A-Z])/g, " $1").replace(/^./, (letter) => letter.toUpperCase());
}

function toJson(value: unknown): string {
  return typeof value === "string" ? value : JSON.stringify(value, null, 2);
}

function parseJsonOrText(value: string): unknown {
  try {
    return JSON.parse(value) as unknown;
  } catch {
    return value;
  }
}
