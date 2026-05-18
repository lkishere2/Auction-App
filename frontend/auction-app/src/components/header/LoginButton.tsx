import { useNavigate } from 'react-router-dom';

export default function LoginButton() {
    const navigate = useNavigate();

    return (
        <button
            onClick={() => navigate('/login')}
            style={buttonStyle}
            onMouseEnter={(e) => e.currentTarget.style.setProperty('opacity', '0.85')}
            onMouseLeave={(e) => e.currentTarget.style.setProperty('opacity', '1')}
        >
            Log in
        </button>
    );
}

const buttonStyle = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '36px',
    padding: '0 20px',
    background: '#0D0D0D',
    color: '#FFFFFF',
    border: 'none',
    borderRadius: '999px',
    fontSize: '13px',
    fontWeight: 600,
    letterSpacing: '0.01em',
    cursor: 'pointer',
    fontFamily: 'inherit',
    transition: 'opacity 0.15s ease',
};