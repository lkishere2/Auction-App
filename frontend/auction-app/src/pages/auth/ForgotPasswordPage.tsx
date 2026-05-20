import { useState } from 'react';
import ForgotPasswordBox from './ForgotPasswordBox';
import ErrorBox from '../../components/ErrorBox';

export default function ForgotPasswordPage() {
    const [error, setError] = useState<{ title: string; message: string } | null>(null);

    return (
        <main className="w-screen h-screen min-h-screen flex items-center justify-center bg-gray-50 overflow-hidden select-none relative">
            <ForgotPasswordBox onError={(title, message) => setError({ title, message })} />

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