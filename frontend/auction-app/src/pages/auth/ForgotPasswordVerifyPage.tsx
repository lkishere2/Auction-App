import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import ForgotPasswordVerifyBox from './ForgotPasswordVerifyBox';
import ErrorBox from '../../components/ErrorBox';

export default function ForgotPasswordVerifyPage() {
    const location = useLocation();
    const navigate = useNavigate();
    const [error, setError] = useState<{ title: string; message: string } | null>(null);
    const email = location.state?.email;

    useEffect(() => {
        if (!email) {
            navigate('/forget-password');
        }
    }, [email, navigate]);

    if (!email) return null;

    return (
        <main className="w-screen h-screen min-h-screen flex items-center justify-center bg-gray-50 overflow-hidden select-none relative">
            <ForgotPasswordVerifyBox email={email} onError={(title, message) => setError({ title, message })} />

            {error && (
                <ErrorBox
                    title={error.title}
                    message={error.message}
                    onClose={() => setError(null)}
                />
            )}
        </main>
    );
}