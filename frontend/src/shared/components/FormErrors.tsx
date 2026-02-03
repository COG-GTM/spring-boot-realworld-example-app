interface FormErrorsProps {
  errors?: { [key: string]: string[] | undefined };
}

export function FormErrors({ errors }: FormErrorsProps) {
  if (!errors || Object.keys(errors).length === 0) {
    return null;
  }

  return (
    <ul className="text-red-600 list-disc list-inside mb-4">
      {Object.entries(errors).map(([field, messages]) =>
        messages?.map((message, index) => (
          <li key={`${field}-${index}`}>
            {field} {message}
          </li>
        ))
      )}
    </ul>
  );
}
