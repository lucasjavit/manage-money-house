import { useEffect, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { useConfirm } from '../context/ConfirmContext';
import { ptoService } from '../services/ptoService';
import type { PtoBalance, PtoVacation } from '../types';

const formatDays = (v: number) =>
  new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v);

const formatDate = (iso: string) => {
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
};

const todayIso = () => new Date().toISOString().slice(0, 10);

const LucasIT = () => {
  const { user } = useAuth();
  const confirm = useConfirm();

  const [balance, setBalance] = useState<PtoBalance | null>(null);
  const [vacations, setVacations] = useState<PtoVacation[]>([]);
  const [hasConfig, setHasConfig] = useState(false);
  const [loading, setLoading] = useState(true);

  // Config
  const [baseDate, setBaseDate] = useState(todayIso());
  const [initialBalance, setInitialBalance] = useState('');
  const [country, setCountry] = useState<'BR' | 'US'>('BR');

  // Projeção
  const [projDate, setProjDate] = useState('');
  const [projBalance, setProjBalance] = useState<PtoBalance | null>(null);

  // Férias
  const [vacStart, setVacStart] = useState('');
  const [vacEnd, setVacEnd] = useState('');
  const [vacDesc, setVacDesc] = useState('');

  const load = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const cfg = await ptoService.getConfig(user.id);
      if (cfg) {
        setHasConfig(true);
        setBaseDate(cfg.baseDate);
        setInitialBalance(String(cfg.initialBalance));
        setCountry(cfg.country);
        setBalance(await ptoService.getBalance(user.id));
        setVacations(await ptoService.getVacations(user.id));
      } else {
        setHasConfig(false);
      }
    } catch (e) {
      console.error('Erro ao carregar PTO:', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user]);

  const saveConfig = async () => {
    if (!user) return;
    const num = parseFloat(initialBalance.replace(',', '.'));
    if (isNaN(num) || num < 0) {
      alert('Informe um saldo inicial válido');
      return;
    }
    try {
      await ptoService.saveConfig({ userId: user.id, baseDate, initialBalance: num, country });
      await load();
    } catch (e) {
      console.error(e);
      alert('Erro ao salvar configuração');
    }
  };

  const project = async () => {
    if (!user || !projDate) return;
    try {
      setProjBalance(await ptoService.getBalance(user.id, projDate));
    } catch (e) {
      console.error(e);
    }
  };

  const addVacation = async () => {
    if (!user || !vacStart || !vacEnd) {
      alert('Informe início e fim das férias');
      return;
    }
    try {
      await ptoService.createVacation({
        userId: user.id,
        startDate: vacStart,
        endDate: vacEnd,
        description: vacDesc.trim() || undefined,
      });
      setVacStart('');
      setVacEnd('');
      setVacDesc('');
      await load();
    } catch (e) {
      console.error(e);
      alert('Erro ao registrar férias (verifique o período)');
    }
  };

  const removeVacation = async (id: number) => {
    const ok = await confirm({
      title: 'Excluir férias?',
      message: 'Este período será removido e o saldo recalculado.',
    });
    if (!ok) return;
    try {
      await ptoService.deleteVacation(id);
      await load();
    } catch (e) {
      console.error(e);
      alert('Erro ao excluir');
    }
  };

  if (loading) {
    return <div className="max-w-3xl mx-auto p-6 text-center text-slate-400">Carregando...</div>;
  }

  const progressPct = balance ? Math.round(balance.fractionToNextPto * 100) : 0;

  return (
    <div className="max-w-3xl mx-auto p-6 space-y-6">
      <h2 className="text-2xl font-bold text-slate-800">PTO Aditi</h2>

      {/* Saldo atual */}
      {hasConfig && balance && (
        <div className="bg-gradient-to-br from-blue-600 to-blue-800 rounded-2xl p-6 shadow-lg text-white">
          <p className="text-sm text-blue-100">Saldo de PTO hoje</p>
          <p className="text-4xl font-bold mt-1">{formatDays(balance.balance)} dias</p>
          <div className="mt-4">
            <div className="flex justify-between text-xs text-blue-100 mb-1">
              <span>Progresso para o próximo dia</span>
              <span>faltam {balance.daysToNextPto} dias</span>
            </div>
            <div className="w-full h-2 bg-blue-900/40 rounded-full overflow-hidden">
              <div className="h-full bg-white/90 rounded-full" style={{ width: `${progressPct}%` }} />
            </div>
          </div>
          <p className="text-xs text-blue-100 mt-3">
            Inicial {formatDays(balance.initialBalance)} + acumulado {formatDays(balance.accruedSinceBase)}
            {' '}− férias {formatDays(balance.usedVacationDays)} · calendário {balance.country}
          </p>
        </div>
      )}

      {/* Configuração */}
      <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 p-5">
        <h3 className="text-lg font-semibold text-slate-800 mb-1">Parâmetros do PTO</h3>
        <p className="text-sm text-slate-600 mb-4">
          Informe o saldo que você já tem numa data-base. A partir daí, 1 dia a cada 25 dias corridos.
        </p>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Data-base</label>
            <input type="date" value={baseDate} onChange={(e) => setBaseDate(e.target.value)}
              className="w-full px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Saldo nessa data (dias)</label>
            <input type="text" inputMode="decimal" value={initialBalance}
              onChange={(e) => setInitialBalance(e.target.value)} placeholder="10"
              className="w-full px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
          </div>
          <div>
            <label className="block text-xs font-semibold text-slate-600 mb-1">Feriados</label>
            <select value={country} onChange={(e) => setCountry(e.target.value as 'BR' | 'US')}
              className="w-full px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white text-slate-800">
              <option value="BR">Brasil (federais)</option>
              <option value="US">Estados Unidos</option>
            </select>
          </div>
        </div>
        <button onClick={saveConfig}
          className="mt-4 px-4 py-2 text-sm font-semibold text-white bg-blue-700 rounded-lg hover:bg-blue-800">
          Salvar parâmetros
        </button>
      </div>

      {hasConfig && (
        <>
          {/* Projeção */}
          <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 p-5">
            <h3 className="text-lg font-semibold text-slate-800 mb-3">Projeção futura</h3>
            <div className="flex flex-wrap items-end gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Numa data futura</label>
                <input type="date" value={projDate} onChange={(e) => setProjDate(e.target.value)}
                  className="px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
              </div>
              <button onClick={project}
                className="px-4 py-2 text-sm font-semibold text-blue-700 border-2 border-blue-300 rounded-lg hover:bg-blue-50">
                Calcular
              </button>
              {projBalance && (
                <span className="text-sm text-slate-700">
                  Em {formatDate(projBalance.date)}: <strong>{formatDays(projBalance.balance)} dias</strong>
                </span>
              )}
            </div>
          </div>

          {/* Férias */}
          <div className="bg-white/80 rounded-2xl border-2 border-slate-200/60 p-5">
            <h3 className="text-lg font-semibold text-slate-800 mb-3">Férias</h3>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Início</label>
                <input type="date" value={vacStart} onChange={(e) => setVacStart(e.target.value)}
                  className="w-full px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Fim</label>
                <input type="date" value={vacEnd} onChange={(e) => setVacEnd(e.target.value)}
                  className="w-full px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">Descrição (opcional)</label>
                <input type="text" value={vacDesc} onChange={(e) => setVacDesc(e.target.value)}
                  className="w-full px-3 py-2 text-sm border-2 border-slate-300 rounded-lg bg-white" />
              </div>
            </div>
            <button onClick={addVacation}
              className="mt-4 px-4 py-2 text-sm font-semibold text-white bg-blue-700 rounded-lg hover:bg-blue-800">
              Adicionar férias
            </button>

            <div className="mt-4 space-y-2">
              {vacations.length === 0 ? (
                <p className="text-sm text-slate-400">Nenhuma férias registrada.</p>
              ) : (
                vacations.map((v) => (
                  <div key={v.id} className="flex items-center gap-3 px-4 py-3 rounded-lg border border-slate-200 bg-white group">
                    <div className="min-w-0 flex-1">
                      <p className="text-sm font-medium text-slate-800">
                        {formatDate(v.startDate)} → {formatDate(v.endDate)}
                        {v.description ? ` · ${v.description}` : ''}
                      </p>
                      <p className="text-xs text-slate-500">{v.businessDaysUsed} dia(s) útil(eis) de PTO</p>
                    </div>
                    <button onClick={() => removeVacation(v.id)}
                      className="opacity-0 group-hover:opacity-100 transition-opacity text-slate-300 hover:text-red-600 text-lg font-bold">
                      ×
                    </button>
                  </div>
                ))
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default LucasIT;
