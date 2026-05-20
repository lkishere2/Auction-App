import { NavLink } from 'react-router-dom';

interface NavItemProps {
    label: string;
    to: string;
}

export default function NavItem({ label, to }: NavItemProps) {
    return (
        <NavLink
            to={to}
            style={({ isActive }) => ({
                position: 'relative',
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                padding: '0 20px',
                height: '72px',
                fontSize: '16px',
                fontWeight: 600,
                color: '#0D0D0D',
                textDecoration: 'none',
                letterSpacing: '-0.01em',
                borderBottom: isActive ? '3px solid #F5C518' : '3px solid transparent',
                transition: 'border-color 0.15s ease',
            })}
        >
            {label}
        </NavLink>
    );
}