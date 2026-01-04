import React, { createContext, useContext, useState, useEffect } from 'react';
import type { User } from '../types';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  login: (email: string) => Promise<boolean>;
  logout: () => void;
  switchUser: () => Promise<void>;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);

  useEffect(() => {
    const savedUser = localStorage.getItem('user');
    if (savedUser) {
      setUser(JSON.parse(savedUser));
    }
  }, []);

  const login = async (email: string): Promise<boolean> => {
    try {
      const userData = await authService.login(email);
      setUser(userData);
      localStorage.setItem('user', JSON.stringify(userData));
      return true;
    } catch (error) {
      console.error('Login error:', error);
      return false;
    }
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem('user');
  };

  const switchUser = async () => {
    if (!user) return;
    
    // Emails dos usuários
    const lucasEmail = 'vyeiralucas@gmail.com';
    const marianaEmail = 'marii_borges@hotmail.com';
    
    // Determinar qual é o outro usuário
    const otherEmail = user.email === lucasEmail ? marianaEmail : lucasEmail;
    
    // Fazer login com o outro usuário
    await login(otherEmail);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout, switchUser, isAuthenticated: !!user }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

