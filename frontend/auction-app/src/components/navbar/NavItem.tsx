import { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

interface SubItem {
  label: string;
  path: string;
}

interface NavItemProps {
  label: string;
  path?: string;
  icon: React.ComponentType<{ className?: string }>;
  subItems?: SubItem[];
  isActive?: boolean;
}

export default function NavItem({ label, path, icon: Icon, subItems, isActive }: NavItemProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);

  const hasSubItems = subItems && subItems.length > 0;
  const isCurrentActive = isActive || (path ? location.pathname === path : false);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    e.preventDefault();
    if (hasSubItems) {
      setIsOpen(!isOpen);
    } else if (path) {
      navigate(path);
    }
  };

  return (
    <div ref={containerRef} className="w-full relative flex flex-col items-center bg-white">
      <button
        onClick={handleClick}
        type="button"
        className={`w-full flex flex-col items-center justify-center py-5 px-1 border-l-4 transition-all duration-200 ${
          isCurrentActive
            ? 'border-[#F5C518] text-[#F5C518] bg-amber-50/40' // Đổi nhẹ nền khi active để dễ nhận diện nút
            : 'border-transparent text-[#0D0D0D] hover:text-[#F5C518] bg-white'
        }`}
      >
        {/* Tách Class riêng cho Icon, không ép pointer-events bừa bãi vào Component gốc */}
        <div className="w-6 h-6 mb-1 flex items-center justify-center text-current">
          <Icon className="w-full h-full" />
        </div>
        <span className="text-[11px] font-semibold text-center tracking-wide block w-full truncate">
          {label}
        </span>
      </button>

      {/* Pop-up Box */}
      {hasSubItems && isOpen && (
        <div className="absolute left-[100px] top-4 w-44 bg-[#FFFFFF] border border-slate-200 rounded-md shadow-xl z-50 py-1">
          <div className="absolute right-full top-4 -mr-[1px] border-8 border-transparent border-r-[#FFFFFF]" />
          <div className="absolute right-full top-4 border-8 border-transparent border-r-slate-200 -z-10" />
          
          {subItems.map((sub, index) => {
            const isSubActive = location.pathname === sub.path;
            return (
              <button
                key={index}
                type="button"
                onClick={(e) => {
                  e.preventDefault();
                  navigate(sub.path);
                  setIsOpen(false);
                }}
                className={`w-full text-left px-4 py-2.5 text-xs font-semibold transition-colors ${
                  isSubActive
                    ? 'text-[#FFFFFF] bg-[#0D0D0D]'
                    : 'text-[#0D0D0D] bg-[#FFFFFF] hover:bg-slate-100'
                }`}
              >
                {sub.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
}