import type { ReactNode } from 'react';

/** Props of {@link Field}. */
export interface FieldProps {
  /** Visible label. */
  label: string;
  /** Id of the input the label points to. */
  htmlFor: string;
  /** Validation message, if any. */
  error?: string;
  /** The input. */
  children: ReactNode;
}

/**
 * Label, input and validation message of a form field.
 *
 * @param props - {@link FieldProps}
 * @returns the field
 */
export function Field({ label, htmlFor, error, children }: FieldProps) {
  return (
    <div className="flex flex-col gap-1">
      <label htmlFor={htmlFor} className="text-sm text-(--color-text-muted)">
        {label}
      </label>
      {children}
      {error && (
        <p role="alert" className="text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  );
}
