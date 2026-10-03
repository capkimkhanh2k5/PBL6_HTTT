import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';
import { MOCK_TEST_ACCOUNTS } from '../mockData';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  token?: string | null;
  login: (userData: User, token?: string) => void;
  logout: () => void;
  switchRole: (roleKey: 'customer' | 'vendor' | 'admin') => User;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: MOCK_TEST_ACCOUNTS.customer,
      isAuthenticated: true,
      token: null,
      login: (userData, token) => {
        if (token) {
          localStorage.setItem('accessToken', token);
        }
        set({ user: userData, isAuthenticated: true, token: token || null });
      },
      logout: () => {
        localStorage.removeItem('accessToken');
        set({ user: null, isAuthenticated: false, token: null });
      },
      switchRole: (roleKey) => {
        const targetUser = MOCK_TEST_ACCOUNTS[roleKey];
        set({ user: targetUser, isAuthenticated: true });
        return targetUser;
      },
    }),
    {
      name: 'danasea_auth_storage',
    }
  )
);
