import { useEffect, useRef } from 'react';
import Globe from './Globe';

export default function Welcome() {
    const titleRef = useRef<HTMLHeadingElement>(null);
    const eyebrowRef = useRef<HTMLParagraphElement>(null);
    const globeWrapperRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const eyebrow = eyebrowRef.current;
        const title = titleRef.current;
        const globe = globeWrapperRef.current;
        if (!eyebrow || !title || !globe) return;

        eyebrow.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
        title.style.transition = 'opacity 1.2s ease, transform 1.2s ease';
        globe.style.transition = 'opacity 1.5s ease, transform 1.5s ease';

        requestAnimationFrame(() => {
            setTimeout(() => {
                eyebrow.style.opacity = '1';
                eyebrow.style.transform = 'translateY(0)';
            }, 200);
            setTimeout(() => {
                title.style.opacity = '1';
                title.style.transform = 'translateY(0)';
            }, 500);
            setTimeout(() => {
                globe.style.opacity = '1';
                globe.style.transform = 'translateX(-50%) translateY(0)';
            }, 800);
        });
    }, []);

    return (
        <section
            style={{
                position: 'relative',
                width: '100%',
                overflow: 'hidden',
                background: '#fff',
                minHeight: '850px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
            }}
        >
            <div
                style={{
                    position: 'relative',
                    zIndex: 2,
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'center',
                    paddingTop: '120px',
                    textAlign: 'center',
                }}
            >
                <p
                    ref={eyebrowRef}
                    style={{
                        fontSize: '16px',
                        letterSpacing: '0.15em',
                        textTransform: 'uppercase',
                        color: '#F5C518',
                        fontWeight: 700,
                        marginBottom: '24px',
                        opacity: 0,
                        transform: 'translateY(20px)',
                    }}
                >
                    Premium bidding experience
                </p>

                <h1
                    ref={titleRef}
                    style={{
                        fontSize: '72px',
                        fontWeight: 900,
                        color: '#0D0D0D',
                        lineHeight: 1.1,
                        letterSpacing: '-0.04em',
                        maxWidth: '1000px',
                        margin: '0 auto',
                        opacity: 0,
                        transform: 'translateY(30px)',
                    }}
                >
                    Discover a world where{' '}
                    <span style={{ color: '#F5C518' }}>every bid leads to excellence</span>
                </h1>
            </div>

            <div
                ref={globeWrapperRef}
                style={{
                    position: 'absolute',
                    bottom: '-400px',
                    left: '50%',
                    transform: 'translateX(-50%) translateY(100px)',
                    zIndex: 1,
                    pointerEvents: 'auto',
                    opacity: 0,
                }}
            >
                <Globe />
            </div>
        </section>
    );
}