interface BadgeProps {
  children: React.ReactNode;
  variant?: 'buy' | 'sell' | 'hold' | 'default';
  size?: 'sm' | 'md' | 'lg';
}

const variantStyles = {
  buy: 'bg-green-100 text-green-800 border-green-300',
  sell: 'bg-red-100 text-red-800 border-red-300',
  hold: 'bg-yellow-100 text-yellow-800 border-yellow-300',
  default: 'bg-gray-100 text-gray-800 border-gray-300',
};

const sizeStyles = {
  sm: 'px-2 py-0.5 text-xs',
  md: 'px-3 py-1 text-sm',
  lg: 'px-4 py-1.5 text-base',
};

export function Badge({ children, variant = 'default', size = 'md' }: BadgeProps) {
  return (
    <span
      className={`inline-flex items-center font-semibold rounded-full border ${variantStyles[variant]} ${sizeStyles[size]}`}
    >
      {children}
    </span>
  );
}
