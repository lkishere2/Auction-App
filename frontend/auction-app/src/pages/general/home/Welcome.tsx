import { useEffect, useRef } from 'react';

export default function Welcome() {
    const titleRef = useRef<HTMLHeadingElement>(null);
    const eyebrowRef = useRef<HTMLParagraphElement>(null);
    const buttonRef = useRef<HTMLButtonElement>(null); // Thêm đúng ref này cho nút

    useEffect(() => {
        const eyebrow = eyebrowRef.current;
        const title = titleRef.current;
        const button = buttonRef.current;
        if (!eyebrow || !title || !button) return;

        eyebrow.style.transition = 'opacity 0.8s ease, transform 0.8s ease';
        title.style.transition = 'opacity 1.2s ease, transform 1.2s ease';
        button.style.transition = 'opacity 1.2s ease, transform 1.2s ease'; // Hiệu ứng cho nút

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
                button.style.opacity = '1';
                button.style.transform = 'translateY(0)';
            }, 800); // Xuất hiện mượt mà sau tiêu đề
        });
    }, []);

    const handleStartNow = () => {
        const auctionSection = document.getElementById('live-showroom-auctions');
        if (auctionSection) {
            auctionSection.scrollIntoView({ behavior: 'smooth' });
        }
    };

    return (
        <section
            style={{
                position: 'relative',
                flex: '1',
                maxWidth: '800px',
                padding: '100px 32px 0 32px',
                minHeight: '750px',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-start',
                textAlign: 'left',
                background: '#fff',
                overflow: 'hidden',
                zIndex: 2,
            }}
        >
            <p
                ref={eyebrowRef}
                style={{
                    fontSize: '18px',
                    letterSpacing: '0.15em',
                    textTransform: 'uppercase',
                    color: '#F5C518',
                    fontWeight: 700,
                    margin: '0 0 2px 0',
                    opacity: 0,
                    transform: 'translateY(20px)',
                }}
            >
                Premium bidding experience
            </p>

            <h1
                ref={titleRef}
                style={{
                    fontSize: '48px',
                    fontWeight: 900,
                    lineHeight: 1.1,
                    letterSpacing: '-0.04em',
                    margin: 0,
                    opacity: 0,
                    transform: 'translateY(30px)',
                }}
            >
                <span style={{ display: 'block', color: '#0D0D0D', whiteSpace: 'nowrap' }}>
                    Discover a world where
                </span>
                <span style={{ display: 'block', color: '#F5C518', marginTop: '1px', whiteSpace: 'nowrap' }}>
                    every bid leads to excellence
                </span>
            </h1>

            {/* CHỈ THÊM ĐÚNG NÚT BẤM NÀY XUỐNG DƯỚI DÒNG TEXT */}
            <button
                ref={buttonRef}
                onClick={handleStartNow}
                style={{
                    marginTop: '40px',
                    padding: '14px 36px',
                    fontSize: '15px',
                    fontWeight: 800,
                    textTransform: 'uppercase',
                    letterSpacing: '0.06em',
                    backgroundColor: '#0D0D0D',
                    color: '#FFFFFF',
                    border: 'none',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    opacity: 0,
                    transform: 'translateY(20px)',
                    transition: 'all 0.2s ease-in-out',
                }}
                onMouseEnter={(e) => {
                    e.currentTarget.style.backgroundColor = '#F5C518';
                    e.currentTarget.style.color = '#0D0D0D';
                }}
                onMouseLeave={(e) => {
                    e.currentTarget.style.backgroundColor = '#0D0D0D';
                    e.currentTarget.style.color = '#FFFFFF';
                }}
            >
                Start now
            </button>
        </section>
    );
}