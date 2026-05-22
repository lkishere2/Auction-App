import { ChevronDown, User, LogOut } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

interface ProfileButtonProps {
    username: string;
    initials: string;
    onLogout?: () => void;
}

export default function ProfileButton({ username, initials, onLogout }: ProfileButtonProps) {
    const [open, setOpen] = useState(false);
    const ref = useRef<HTMLDivElement>(null);
    const navigate = useNavigate();

    useEffect(() => {
        function handleClickOutside(e: MouseEvent) {
            if (ref.current && !ref.current.contains(e.target as Node)) {
                setOpen(false);
            }
        }
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    return (
        <div ref={ref} style={{ position: 'relative' }}>
            <button
                onClick={() => setOpen((prev) => !prev)}
                style={{
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '10px',
                    height: '44px',
                    padding: '0 16px 0 8px',
                    background: '#FFFFFF',
                    border: '1px solid #0D0D0D',
                    borderRadius: '999px',
                    cursor: 'pointer',
                    fontFamily: 'inherit',
                    transition: 'background 0.15s ease',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.background = '#F9F9F9')}
                onMouseLeave={(e) => (e.currentTarget.style.background = '#FFFFFF')}
            >
                <div
                    style={{
                        width: '32px',
                        height: '32px',
                        borderRadius: '50%',
                        background: '#F5C518',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontSize: '12px',
                        fontWeight: 700,
                        color: '#0D0D0D',
                        letterSpacing: '0.02em',
                        flexShrink: 0,
                    }}
                >
                    {initials}
                </div>

                <span
                    style={{
                        fontSize: '15px',
                        fontWeight: 500,
                        color: '#0D0D0D',
                        maxWidth: '120px',
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                    }}
                >
                    {username}
                </span>

                <ChevronDown
                    size={16}
                    color="#0D0D0D"
                    strokeWidth={2}
                    style={{
                        transform: open ? 'rotate(180deg)' : 'rotate(0deg)',
                        transition: 'transform 0.2s ease',
                    }}
                    aria-hidden
                />
            </button>

            {open && (
                <div
                    style={{
                        position: 'absolute',
                        top: 'calc(100% + 8px)',
                        right: 0,
                        minWidth: '180px',
                        background: '#FFFFFF',
                        border: '1px solid #0D0D0D',
                        borderRadius: '12px',
                        overflow: 'hidden',
                        zIndex: 50,
                    }}
                >
                    <button
                        onClick={() => { navigate('/profile'); setOpen(false); }}
                        style={dropdownItemStyle}
                        onMouseEnter={(e) => (e.currentTarget.style.background = '#F5C51820')}
                        onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                    >
                        <User size={16} strokeWidth={2} aria-hidden />
                        My Profile
                    </button>

                    <div style={{ height: '0.5px', background: '#0D0D0D20', margin: '0 12px' }} />

                    <button
                        onClick={() => { 
                            onLogout?.(); 
                            setOpen(false);
                            navigate('/');
                            window.location.reload();
                        }}
                        style={{ ...dropdownItemStyle, color: '#CC0000' }}
                        onMouseEnter={(e) => (e.currentTarget.style.background = '#FFF0F0')}
                        onMouseLeave={(e) => (e.currentTarget.style.background = 'transparent')}
                    >
                        <LogOut size={16} strokeWidth={2} aria-hidden />
                        Log out
                    </button>
                </div>
            )}
        </div>
    );
}

const dropdownItemStyle: React.CSSProperties = {
    display: 'flex',
    alignItems: 'center',
    gap: '12px',
    width: '100%',
    padding: '12px 20px',
    background: 'transparent',
    border: 'none',
    fontSize: '15px',
    fontWeight: 500,
    color: '#0D0D0D',
    cursor: 'pointer',
    fontFamily: 'inherit',
    textAlign: 'left',
    transition: 'background 0.12s ease',
};