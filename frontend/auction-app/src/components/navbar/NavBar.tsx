import { 
  LayoutDashboard, 
  Compass, 
  Store, 
  UserCheck, 
  User, 
  LogOut, 
  LogIn
} from 'lucide-react';
import { useLocation, useNavigate } from 'react-router-dom';
import NavItem from './NavItem';

interface NavBarProps {
  userId: string | number;
  isLoggedIn: boolean;
  onLogout: () => void;
  onLogin: () => void;
}

export default function NavBar({ userId, isLoggedIn, onLogout, onLogin }: NavBarProps) {
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogoutClick = () => {
    onLogout();
    navigate('/');
  };

  return (
    // Đã chỉnh sửa: Thay top-0 thành top-[70px] và h-screen thành h-[calc(100vh-70px)] để khớp dưới Header
    <aside className="w-[100px] h-[calc(100vh-70px)] bg-[#FFFFFF] border-r border-slate-200 flex flex-col justify-between items-center shadow-sm select-none fixed left-0 top-[70px] overflow-y-visible z-40">
      
      <div className="w-full flex flex-col items-center pt-2 bg-[#FFFFFF]">
        
        {/* Nút Dashboard nằm trên cùng dẫn về /office */}
        <NavItem 
          label="Dashboard" 
          path="/office" 
          icon={LayoutDashboard}
          isActive={location.pathname === '/office' || location.pathname === '/'}
        />

        {/* Explore */}
        <NavItem 
          label="Explore" 
          path="/explore" 
          icon={Compass} 
        />

        {/* Market */}
        <NavItem 
          label="Market" 
          icon={Store} 
          isActive={location.pathname.startsWith('/auction') || location.pathname.startsWith('/auctions')}
          subItems={[
            { label: 'All Auctions', path: '/auctions/hub' },
            { label: 'My Bids', path: '/auction/joined' }
          ]}
        />

        {/* Account */}
        <NavItem 
          label="Account" 
          path="/account" 
          icon={UserCheck} 
        />

        {/* Profile */}
        <NavItem 
          label="Profile" 
          path={`/profile/${userId}`} 
          icon={User} 
        />
      </div>

      {/* Bottom Section giữ nguyên */}
      <div className="w-full border-t border-slate-100 bg-[#FFFFFF]">
        {isLoggedIn ? (
          <button
            onClick={handleLogoutClick}
            type="button"
            className="w-full flex flex-col items-center justify-center py-5 px-1 text-red-500 bg-[#FFFFFF] hover:text-red-600 transition-colors border-l-4 border-transparent"
          >
            <LogOut className="w-6 h-6 mb-1" />
            <span className="text-[11px] font-semibold">Logout</span>
          </button>
        ) : (
          <button
            onClick={onLogin}
            type="button"
            className="w-full flex flex-col items-center justify-center py-5 px-1 text-[#0D0D0D] bg-[#FFFFFF] hover:text-[#F5C518] transition-colors border-l-4 border-transparent"
          >
            <LogIn className="w-6 h-6 mb-1" />
            <span className="text-[11px] font-semibold">Login</span>
          </button>
        )}
      </div>
    </aside>
  );
}