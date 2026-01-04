import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

const Login = () => {
  const [email, setEmail] = useState('marii_borges@hotmail.com');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();

  const users = [
    { email: 'vyeiralucas@gmail.com', name: 'Lucas' },
    { email: 'marii_borges@hotmail.com', name: 'Mariana' }
  ];

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!email) {
      setError('Por favor, selecione um usuário');
      return;
    }

    setLoading(true);
    const success = await login(email);
    if (!success) {
      setError('Erro ao fazer login. Tente novamente.');
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50/50 via-white to-pink-50/50 p-4">
      <div className="max-w-md w-full space-y-8 p-10 bg-white/90 backdrop-blur-sm rounded-2xl shadow-xl border-2 border-slate-200/60">
        <div className="text-center">
          <div className="mx-auto w-16 h-16 bg-gradient-to-br from-blue-500 via-blue-600 to-pink-500 rounded-2xl flex items-center justify-center mb-4 shadow-lg ring-2 ring-blue-500/20">
            <span className="text-2xl font-bold text-white">$</span>
          </div>
          <h2 className="text-3xl font-extrabold bg-gradient-to-r from-blue-600 via-blue-700 to-pink-600 bg-clip-text text-transparent mb-2">
            Manage House Money
          </h2>
          <p className="text-sm text-slate-600 font-medium">
            Selecione seu usuário para fazer login
          </p>
        </div>
        <form className="space-y-6" onSubmit={handleSubmit}>
          <div>
            <label htmlFor="email" className="block text-sm font-semibold text-slate-700 mb-2">
              Usuário
            </label>
            <select
              id="email"
              name="email"
              required
              className="appearance-none w-full px-4 py-3 border-2 border-slate-300 rounded-xl text-slate-900 focus:outline-none focus:ring-2 focus:ring-blue-400/50 focus:border-blue-500 transition-all bg-white shadow-sm hover:border-slate-400 font-medium"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            >
              {users.map((user) => (
                <option key={user.email} value={user.email}>
                  {user.name} ({user.email})
                </option>
              ))}
            </select>
          </div>

          {error && (
            <div className="p-3 bg-red-50/80 border-2 border-red-200/60 rounded-xl text-red-700 text-sm text-center font-medium">
              {error}
            </div>
          )}

          <div>
            <button
              type="submit"
              disabled={loading || !email}
              className="w-full flex justify-center items-center py-3 px-4 border border-transparent text-base font-semibold rounded-xl text-white bg-gradient-to-r from-blue-600 via-blue-600 to-blue-700 hover:from-blue-700 hover:via-blue-700 hover:to-blue-800 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-400/50 disabled:opacity-50 disabled:cursor-not-allowed shadow-lg hover:shadow-xl transition-all duration-200 ring-2 ring-blue-500/20"
            >
              {loading ? (
                <>
                  <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Entrando...
                </>
              ) : (
                'Entrar'
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;

