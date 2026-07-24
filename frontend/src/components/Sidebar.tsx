import { Link, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { useAuth } from '../context/AuthContext'

interface NavItem {
  path: string
  label: string
  icon: React.ReactNode
}

// Grupo "Lucas": só o Lucas vê. Restrição visual (o backend segue aberto).
// Dividido em dois sub-grupos retráteis.
const LUCAS_EMAIL = 'vyeiralucas@gmail.com'
const PESSOAL_PATHS = ['/extract', '/lucas-gastos', '/salary', '/investments']
const LUCAS_IT_PATHS = ['/lucas-it']
const LUCAS_PATHS = [...PESSOAL_PATHS, ...LUCAS_IT_PATHS]

interface SidebarProps {
  isOpen?: boolean
  onClose?: () => void
  onToggle?: () => void
}

const Sidebar = ({ isOpen: controlledIsOpen, onClose, onToggle }: SidebarProps) => {
  const location = useLocation()
  const { user } = useAuth()
  const [isOpen, setIsOpen] = useState(false)
  const isLucas = user?.email === LUCAS_EMAIL
  // Grupos começam expandidos se a rota atual for de uma das abas contidas.
  const [lucasOpen, setLucasOpen] = useState(() => LUCAS_PATHS.includes(location.pathname))
  const [pessoalOpen, setPessoalOpen] = useState(() => PESSOAL_PATHS.includes(location.pathname))
  const [itOpen, setItOpen] = useState(() => LUCAS_IT_PATHS.includes(location.pathname))

  const sidebarOpen = controlledIsOpen !== undefined ? controlledIsOpen : isOpen
  
  useEffect(() => {
    // Fechar sidebar ao mudar de rota em mobile
    if (window.innerWidth < 1024 && sidebarOpen && onClose) {
      onClose()
    }
  }, [location.pathname])
  
  const handleClose = () => {
    if (onClose) {
      onClose()
    } else {
      setIsOpen(false)
    }
  }

  const navItems: NavItem[] = [
    {
      path: '/',
      label: 'Planilha de Despesas',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
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
      ),
    },
    {
      path: '/extract',
      label: 'Cartão de Crédito',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z"
          />
        </svg>
      ),
    },
    {
      path: '/lucas-gastos',
      label: 'Lucas - Gastos',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
          />
        </svg>
      ),
    },
    {
      path: '/lucas-it',
      label: 'PTO Aditi',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z"
          />
        </svg>
      ),
    },
    {
      path: '/salary',
      label: 'Salários',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
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
      ),
    },
    {
      path: '/investments',
      label: 'Investimentos',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
          fill="none"
          viewBox="0 0 24 24"
          stroke="currentColor"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
          />
        </svg>
      ),
    },
    {
      path: '/settings',
      label: 'Configurações',
      icon: (
        <svg
          xmlns="http://www.w3.org/2000/svg"
          className="h-5 w-5"
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
      ),
    },
  ]

  const isActive = (path: string) => {
    if (path === '/') {
      return location.pathname === '/'
    }
    return location.pathname.startsWith(path)
  }

  return (
    <>
      {/* Overlay para mobile */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/50 z-30 lg:hidden"
          onClick={onToggle || handleClose}
        />
      )}
      
      <aside
        className={`fixed left-0 top-0 h-screen w-64 bg-gradient-to-b from-blue-600 to-blue-700 shadow-xl border-r border-blue-800/20 z-40 transform transition-transform duration-300 ease-in-out ${
          sidebarOpen ? 'translate-x-0' : '-translate-x-full'
        }`}
      >
        <div className="flex flex-col h-full">
          {/* Logo/Header */}
          <div className="p-6 border-b border-blue-800/20">
            <Link
              to="/"
              className="flex items-center gap-3 hover:opacity-90 transition-opacity"
              onClick={handleClose}
            >
              <div className="w-10 h-10 bg-white/20 backdrop-blur-sm rounded-lg flex items-center justify-center shadow-md ring-2 ring-white/30">
                <span className="text-xl font-bold text-white">$</span>
              </div>
              <h1 className="text-lg font-bold text-white">
                Manage House Money
              </h1>
            </Link>
          </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 space-y-2 overflow-y-auto">
          {(() => {
            const renderLink = (item: NavItem) => {
              const active = isActive(item.path)
              return (
                <Link
                  key={item.path}
                  to={item.path}
                  onClick={handleClose}
                  className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-all duration-200 ${
                    active
                      ? 'bg-white/20 backdrop-blur-sm text-white shadow-md ring-2 ring-white/30'
                      : 'text-white/80 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  <div className={`flex-shrink-0 ${active ? 'text-white' : 'text-white/70'}`}>
                    {item.icon}
                  </div>
                  <span className="font-medium">{item.label}</span>
                </Link>
              )
            }

            // Botão de grupo retrátil (usado para "Lucas", "Pessoal" e "Lucas It").
            const groupToggle = (label: string, open: boolean, toggle: () => void) => (
              <button
                onClick={toggle}
                className="w-full flex items-center gap-3 px-4 py-3 rounded-lg text-white/80 hover:bg-white/10 hover:text-white transition-all duration-200"
              >
                <span className="font-medium flex-1 text-left">{label}</span>
                <svg
                  xmlns="http://www.w3.org/2000/svg"
                  className={`h-4 w-4 transition-transform ${open ? 'rotate-90' : ''}`}
                  fill="none" viewBox="0 0 24 24" stroke="currentColor"
                >
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                </svg>
              </button>
            )

            const generalItems = navItems.filter((i) => !LUCAS_PATHS.includes(i.path))
            const pessoalItems = navItems.filter((i) => PESSOAL_PATHS.includes(i.path))
            const itItems = navItems.filter((i) => LUCAS_IT_PATHS.includes(i.path))
            const settings = generalItems.filter((i) => i.path === '/settings')
            const topItems = generalItems.filter((i) => i.path !== '/settings')

            return (
              <>
                {topItems.map(renderLink)}

                {/* Grupo Lucas — retrátil, visível só para o Lucas, com sub-grupos */}
                {isLucas && (
                  <div>
                    {groupToggle('Lucas', lucasOpen, () => setLucasOpen((v) => !v))}
                    {lucasOpen && (
                      <div className="ml-3 mt-1 pl-2 border-l border-white/20 space-y-1">
                        {/* Sub-grupo Pessoal */}
                        {groupToggle('Pessoal', pessoalOpen, () => setPessoalOpen((v) => !v))}
                        {pessoalOpen && (
                          <div className="ml-3 pl-2 border-l border-white/15 space-y-1">
                            {pessoalItems.map(renderLink)}
                          </div>
                        )}
                        {/* Sub-grupo Lucas It */}
                        {groupToggle('Lucas It', itOpen, () => setItOpen((v) => !v))}
                        {itOpen && (
                          <div className="ml-3 pl-2 border-l border-white/15 space-y-1">
                            {itItems.map(renderLink)}
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                )}

                {settings.map(renderLink)}
              </>
            )
          })()}
        </nav>
      </div>
    </aside>
    </>
  )
}

export default Sidebar

