import { Gavel } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import LoginButton from './LoginButton';
import ProfileButton from './ProfileButton';

interface HeaderProps {
    user?: {
        username: string;
        initials: string;
    };
    onLogout?: () => void;
}

export default function Header({ user, onLogout }: HeaderProps) {
    const navigate = useNavigate();

    return (
        <header
            style={{
                position: 'sticky',
                top: 0,
                zIndex: 100,
                width: '100%',
                height: '72px',
                background: '#FFFFFF',
                borderBottom: '1px solid #0D0D0D',
            }}
        >
            <div
                style={{
                    maxWidth: '1280px',
                    height: '100%',
                    margin: '0 auto',
                    padding: '0 32px',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                }}
            >
                <button
                    onClick={() => navigate('/')}
                    aria-label="BidVault home"
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '12px',
                        background: 'none',
                        border: 'none',
                        cursor: 'pointer',
                        padding: 0,
                        flexShrink: 0,
                    }}
                >
                    <div
                        style={{
                            width: '40px',
                            height: '40px',
                            background: '#0D0D0D',
                            borderRadius: '10px',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            flexShrink: 0,
                        }}
                    >
                        <Gavel size={20} color="#F5C518" strokeWidth={2} aria-hidden />
                    </div>
                    <span
                        style={{
                            fontSize: '22px',
                            fontWeight: 700,
                            color: '#0D0D0D',
                            letterSpacing: '-0.03em',
                            lineHeight: 1,
                        }}
                    >
                        BidVault
                    </span>
                </button>

                <div style={{ display: 'flex', alignItems: 'center' }}>
                    {user ? (
                        <ProfileButton
                            username={user.username}
                            initials={user.initials}
                            onLogout={onLogout}
                        />
                    ) : (
                        <LoginButton />
                    )}
                </div>
            </div>
        </header>
    );
}