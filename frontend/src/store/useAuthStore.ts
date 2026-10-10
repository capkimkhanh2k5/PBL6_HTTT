import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { User } from '../types';
import { MOCK_TEST_ACCOUNTS } from '../mockData';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  login: (userData: User) => void;
  logout: () => void;
  switchRole: (roleKey: 'customer' | 'vendor' | 'admin') => User;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: MOCK_TEST_ACCOUNTS.customer,
      isAuthenticated: true,
      login: (userData) => set({ user: userData, isAuthenticated: true }),
      logout: () => set({ user: null, isAuthenticated: false }),
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
