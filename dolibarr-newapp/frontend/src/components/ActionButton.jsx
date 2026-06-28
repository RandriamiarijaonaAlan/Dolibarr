export default function ActionButton({
  type = 'button',
  variant = 'primary',
  disabled = false,
  children,
  onClick,
}) {
  return (
    <button
      className={`action-button action-button--${variant}`}
      type={type}
      disabled={disabled}
      onClick={onClick}
    >
      {children}
    </button>
  );
}
