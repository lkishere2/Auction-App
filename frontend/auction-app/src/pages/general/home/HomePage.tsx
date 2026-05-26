import { useEffect, useRef } from 'react';
import Footer from '../../../components/footer/Footer';
import Welcome from './Welcome';
import AuctionPreview from './AuctionPreview';
import Globe from './Globe';

export default function HomePage() {
    const headerRef = useRef<HTMLDivElement>(null);
    const globeWrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if ('scrollRestoration' in window.history) {
            window.history.scrollRestoration = 'manual';
        }

        window.scrollTo(0, 0);
        document.body.style.overflow = 'hidden';

        const header = headerRef.current;
        if (header) {
            header.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
            requestAnimationFrame(() => {
                setTimeout(() => {
                    header.style.opacity = '1';
                    header.style.transform = 'translateY(0)';
                }, 100);
            });
        }

        const globe = globeWrapperRef.current;
        if (globe) {
            globe.style.transition = 'opacity 1.5s ease, transform 1.5s ease';
            requestAnimationFrame(() => {
                setTimeout(() => {
                    globe.style.opacity = '1';
                    globe.style.transform = 'translateY(0)';
                }, 800);
            });
        }

        const unlockScroll = setTimeout(() => {
            document.body.style.overflow = 'unset';
        }, 2000);

        return () => {
            clearTimeout(unlockScroll);
            document.body.style.overflow = 'unset';
        };
    }, []);

    return (
        <div className="min-h-screen bg-white flex flex-col">
            <main className="flex-1 relative">
                <div style={{
                    position: 'relative',
                    width: '100%',
                    maxWidth: '1280px',
                    margin: '0 auto',
                    display: 'flex',
                    alignItems: 'flex-start',
                    justifyContent: 'space-between'
                }}>
                    <Welcome />
                    
                    <div
                        ref={globeWrapperRef}
                        style={{
                            flex: '1',
                            display: 'flex',
                            justifyContent: 'center',
                            alignItems: 'center',
                            position: 'relative',
                            marginRight: '-150px',
                            marginTop: '20px',
                            zIndex: 1,
                            pointerEvents: 'auto',
                            opacity: 0,
                            transform: 'translateY(20px)',
                        }}
                    >
                        <div style={{ transform: 'scale(0.85)', transformOrigin: 'center' }}>
                            <Globe />
                        </div>
                    </div>
                </div>

                <AuctionPreview />
            </main>
            <Footer />
        </div>
    );
}