import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom'
import { AuthProvider, useAuth } from './context/AuthContext'
import Login from './components/Login'
import ExpenseSheet from './components/ExpenseSheet'
import Settings from './components/Settings'
import ExtractUpload from './components/ExtractUpload'
import SalaryPage from './components/SalaryPage'
import './App.css'

const AppContent = () => {
  const { isAuthenticated, user, logout, switchUser } = useAuth()

  if (!isAuthenticated) {
    return <Login />
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-pink-50/30">
      <header className="bg-gradient-to-r from-blue-600 to-blue-700 shadow-md border-b border-blue-800/20 sticky top-0 z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-2.5">
          <div className="flex justify-between items-center">
            <Link to="/" className="flex items-center gap-2.5 hover:opacity-90 transition-opacity cursor-pointer">
              <div className="w-8 h-8 bg-white/20 backdrop-blur-sm rounded-lg flex items-center justify-center shadow-md ring-2 ring-white/30">
                <span className="text-lg font-bold text-white">$</span>
              </div>
              <h1 className="text-xl font-bold text-white">
                Manage House Money
              </h1>
            </Link>
            <div className="flex items-center gap-2.5">
              <Link
                to="/extract"
                className="px-3 py-1.5 text-xs font-medium text-white bg-white/20 backdrop-blur-sm border-2 border-white/30 rounded-lg hover:bg-white/30 transition-all flex items-center gap-1.5"
                title="Cartão de Crédito"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                  />
                </svg>
                <span>Cartão de Crédito</span>
              </Link>
              <Link
                to="/salary"
                className="px-3 py-1.5 text-xs font-medium text-white bg-white/20 backdrop-blur-sm border-2 border-white/30 rounded-lg hover:bg-white/30 transition-all flex items-center gap-1.5"
                title="Salários"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
                  />
                </svg>
                <span>Salários</span>
              </Link>
              <Link
                to="/settings"
                className="px-3 py-1.5 text-xs font-medium text-white bg-white/20 backdrop-blur-sm border-2 border-white/30 rounded-lg hover:bg-white/30 transition-all flex items-center justify-center"
                title="Configurações"
              >
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className="h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"
                  />
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"
                  />
                </svg>
              </Link>
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
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <Routes>
              <Route path="/" element={<ExpenseSheet />} />
              <Route path="/settings" element={<Settings />} />
              <Route path="/extract" element={<ExtractUpload />} />
              <Route path="/salary" element={<SalaryPage />} />
            </Routes>
      </main>
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

