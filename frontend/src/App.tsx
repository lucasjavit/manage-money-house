import React from 'react'
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Login from './components/Login'
import ExpenseSheet from './components/ExpenseSheet'
import Settings from './components/Settings'
import ExtractUpload from './components/ExtractUpload'
import SalaryPage from './components/SalaryPage'
import Sidebar from './components/Sidebar'
import './App.css'

const AppContent = () => {
  const { isAuthenticated, user, logout, switchUser } = useAuth()
  const [sidebarOpen, setSidebarOpen] = React.useState(true)
  
  const toggleSidebar = () => {
    setSidebarOpen(!sidebarOpen)
  }

  if (!isAuthenticated) {
    return <Login />
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-pink-50/30 flex">
      {/* Sidebar */}
      <Sidebar isOpen={sidebarOpen} onClose={() => setSidebarOpen(false)} onToggle={toggleSidebar} />

      {/* Main Content */}
      <div className={`flex-1 flex flex-col transition-all duration-300 ${sidebarOpen ? 'lg:ml-64' : 'lg:ml-0'}`}>
        {/* Header */}
        <header className="bg-gradient-to-r from-blue-600 to-blue-700 shadow-md border-b border-blue-800/20 sticky top-0 z-10">
          <div className="px-4 sm:px-6 lg:px-8 py-2.5">
            <div className="flex justify-between items-center">
              {/* Botão toggle menu */}
              <button
                onClick={toggleSidebar}
                className="text-white hover:bg-white/20 p-2 rounded-lg transition-colors"
                aria-label={sidebarOpen ? "Fechar menu" : "Abrir menu"}
              >
                {sidebarOpen ? (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-6 w-6"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M6 18L18 6M6 6l12 12"
                    />
                  </svg>
                ) : (
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    className="h-6 w-6"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                  >
                    <path
                      strokeLinecap="round"
                      strokeLinejoin="round"
                      strokeWidth={2}
                      d="M4 6h16M4 12h16M4 18h16"
                    />
                  </svg>
                )}
              </button>
              
              <div className="flex items-center gap-2.5 ml-auto">
                <button
                  onClick={switchUser}
                  className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg border-2 transition-all cursor-pointer hover:opacity-90 ${
                    user?.color === 'blue' 
                      ? 'bg-white/20 backdrop-blur-sm border-white/30 text-white hover:bg-white/30' 
                      : 'bg-pink-500/80 border-pink-400 text-white hover:bg-pink-600/80'
                  }`}
                  title="Clique para trocar de usuário"
                >
                  <div
                    className={`w-2.5 h-2.5 rounded-full shadow-sm ${
                      user?.color === 'blue' ? 'bg-white ring-2 ring-white/50' : 'bg-white ring-2 ring-pink-300'
                    }`}
                  ></div>
                  <span className="text-xs font-semibold">
                    {user?.name}
                  </span>
                </button>
                <button
                  onClick={logout}
                  className="px-3 py-1.5 text-xs font-medium text-blue-700 bg-white border-2 border-white/30 rounded-lg hover:bg-blue-50 hover:border-white/50 transition-all shadow-sm hover:shadow"
                >
                  Sair
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Main Content Area */}
        <main className="flex-1 px-4 sm:px-6 lg:px-8 py-8">
          <Routes>
            <Route path="/" element={<ExpenseSheet />} />
            <Route path="/settings" element={<Settings />} />
            <Route path="/extract" element={<ExtractUpload />} />
            <Route path="/salary" element={<SalaryPage />} />
          </Routes>
        </main>
      </div>
    </div>
  )
}

function App() {
  return (
    <Router>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </Router>
  )
}

export default App

