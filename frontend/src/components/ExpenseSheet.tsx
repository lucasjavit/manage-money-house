import { useEffect, useRef, useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { expenseService } from '../services/expenseService';
import { recurringExpenseService } from '../services/recurringExpenseService';
import { aiService } from '../services/aiService';
import type { Expense, ExpenseRequest, ExpenseType, ExpenseAlertsResponse, AIMonthlyAnalysis } from '../types';
import RecurringExpenseButton from './RecurringExpenseButton';
import HealthScoreGauge from './charts/HealthScoreGauge';
import SpendingTrendChart from './charts/SpendingTrendChart';
import InflationComparisonChart from './charts/InflationComparisonChart';
import SavingsRateGauge from './charts/SavingsRateGauge';
import IncomeVsExpensesChart from './charts/IncomeVsExpensesChart';
import IncomeStabilityChart from './charts/IncomeStabilityChart';

const ExpenseSheet = () => {
  const { user } = useAuth();
  const [year, setYear] = useState(new Date().getFullYear());
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [expenseTypes, setExpenseTypes] = useState<ExpenseType[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingCell, setEditingCell] = useState<{
    month: number;
    expenseTypeId: number;
  } | null>(null);
  const [editValue, setEditValue] = useState<string>('');
  const [selectedCells, setSelectedCells] = useState<Set<string>>(new Set());
  const [selectionMode, setSelectionMode] = useState(false);
  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth() + 1);
  const [paidMonths, setPaidMonths] = useState<Set<string>>(new Set());
  const [showAddTypeModal, setShowAddTypeModal] = useState(false);
  const [newTypeName, setNewTypeName] = useState('');
  const [alerts, setAlerts] = useState<ExpenseAlertsResponse | null>(null);
  const [loadingAlerts, setLoadingAlerts] = useState(false);
  const [showAlerts, setShowAlerts] = useState(false);
  const [aiAnalysis, setAiAnalysis] = useState<AIMonthlyAnalysis | null>(null);
  const [loadingAiAnalysis, setLoadingAiAnalysis] = useState(false);
  const [showAiAnalysisModal, setShowAiAnalysisModal] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const monthNames = [
    'Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun',
    'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez'
  ];

  useEffect(() => {
    loadData();
    // Carregar meses pagos do localStorage
    const savedPaidMonths = localStorage.getItem(`paidMonths_${year}`);
    if (savedPaidMonths) {
      setPaidMonths(new Set(JSON.parse(savedPaidMonths)));
    } else {
      setPaidMonths(new Set());
    }
  }, [year]);

  useEffect(() => {
    // Resetar para o mês atual quando o ano for o ano atual
    const currentYear = new Date().getFullYear();
    if (year === currentYear) {
      setSelectedMonth(new Date().getMonth() + 1);
    }
  }, [year]);

  useEffect(() => {
    // Carregar alertas quando o mês ou ano mudarem
    if (user) {
      loadAlerts();
    }
  }, [selectedMonth, year, user]);

  const loadData = async () => {
    setLoading(true);
    try {
      const [expensesData, typesData] = await Promise.all([
        expenseService.getExpenses(year),
        expenseService.getExpenseTypes(),
      ]);
      setExpenses(expensesData);
      setExpenseTypes(typesData);
    } catch (error) {
      console.error('Error loading data:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadAlerts = async () => {
    if (!user) return;
    setLoadingAlerts(true);
    try {
      const alertsData = await expenseService.getExpenseAlerts(user.id, year, selectedMonth);
      setAlerts(alertsData);

      // Extrair análise AI se disponível
      if (alertsData.aiAnalysis) {
        setAiAnalysis(alertsData.aiAnalysis);
      }
    } catch (error) {
      console.error('Error loading alerts:', error);
    } finally {
      setLoadingAlerts(false);
    }
  };

  const handleGenerateAiAnalysis = async () => {
    if (!user) return;
    setLoadingAiAnalysis(true);
    try {
      const analysis = await aiService.analyzeMonth(user.id, selectedMonth, year);
      setAiAnalysis(analysis);
      setShowAiAnalysisModal(true);
    } catch (error: any) {
      console.error('Error generating AI analysis:', error);

      // Mensagem amigável baseada no tipo de erro
      let errorMessage = '❌ Não foi possível gerar a análise IA.\n\n';

      if (error.code === 'ERR_NETWORK' || error.message?.includes('Network Error')) {
        errorMessage += '🔴 O servidor backend não está respondendo.\n\n';
        errorMessage += '📝 Instruções:\n';
        errorMessage += '1. Abra um terminal na pasta "backend"\n';
        errorMessage += '2. Execute: mvn spring-boot:run\n';
        errorMessage += '3. Aguarde a mensagem "Started ManageHouseMoneyApplication"\n';
        errorMessage += '4. Tente novamente';
      } else if (error.response?.status === 404) {
        errorMessage += '🔍 Endpoint não encontrado (404).\n\n';
        errorMessage += 'Certifique-se de que:\n';
        errorMessage += '• O backend está rodando na porta 3001\n';
        errorMessage += '• O AIAnalysisController está compilado\n';
        errorMessage += '• Execute: mvn spring-boot:run na pasta backend';
      } else if (error.response?.status === 500) {
        errorMessage += '⚠️ Erro interno do servidor.\n\n';
        errorMessage += 'Possíveis causas:\n';
        errorMessage += '• API key do OpenAI não configurada\n';
        errorMessage += '• Erro na geração da análise\n\n';
        errorMessage += 'Verifique os logs do backend para mais detalhes.';
      } else {
        errorMessage += '⚠️ Erro desconhecido.\n\n';
        errorMessage += `Detalhes: ${error.message || 'Erro ao comunicar com o servidor'}\n\n`;
        errorMessage += 'Verifique se o backend está rodando e tente novamente.';
      }

      alert(errorMessage);
    } finally {
      setLoadingAiAnalysis(false);
    }
  };

  const handleAddExpenseType = async () => {
    if (!newTypeName.trim()) {
      alert('Por favor, informe um nome para o tipo de despesa');
      return;
    }

    try {
      await expenseService.createExpenseType(newTypeName.trim());
      setNewTypeName('');
      setShowAddTypeModal(false);
      await loadData();
    } catch (error) {
      console.error('Error creating expense type:', error);
      alert('Erro ao criar tipo de despesa. Verifique se o nome já existe.');
    }
  };

  const handleDeleteExpenseType = async (id: number, name: string) => {
    if (!confirm(`Tem certeza que deseja excluir o tipo "${name}"?`)) {
      return;
    }

    try {
      await expenseService.deleteExpenseType(id);
      await loadData();
    } catch (error: any) {
      console.error('Error deleting expense type:', error);
      const errorMessage = error.response?.data?.error || error.message || 'Erro ao excluir tipo de despesa';
      alert(errorMessage);
    }
  };

  const handleCreateRecurringExpense = async (
    expenseTypeId: number,
    startMonth: number,
    endMonth: number,
    monthlyAmount: number,
    startDateString?: string,
    endDateString?: string
  ) => {
    if (!user) return;

    try {
      let startDate: string;
      let endDate: string;

      if (startDateString && endDateString) {
        // Usar as datas exatas fornecidas
        startDate = startDateString;
        endDate = endDateString;
      } else {
        // Fallback: converter meses para datas (primeiro dia do mês início, último dia do mês fim)
        startDate = `${year}-${String(startMonth).padStart(2, '0')}-01`;
        endDate = new Date(year, endMonth, 0).toISOString().split('T')[0]; // Último dia do mês
      }

      await recurringExpenseService.createRecurringExpense({
        userId: user.id,
        expenseTypeId,
        monthlyAmount,
        startDate,
        endDate,
      });

      await loadData();
    } catch (error) {
      console.error('Error creating recurring expense:', error);
      alert('Erro ao criar dívida recorrente');
      throw error;
    }
  };

  const getCellValue = (month: number, expenseTypeId: number): number => {
    const expense = expenses.find(
      (e) => e.month === month && e.expenseTypeId === expenseTypeId
    );
    return expense ? expense.amount : 0;
  };

  const getCellColor = (month: number, expenseTypeId: number): string => {
    const expense = expenses.find(
      (e) => e.month === month && e.expenseTypeId === expenseTypeId
    );
    if (!expense) return 'bg-white hover:bg-slate-50/50 border-slate-200/60';
    
    return expense.userColor === 'blue' 
      ? 'bg-blue-50/80 border-blue-300/60 hover:bg-blue-100/80 hover:border-blue-400/80' 
      : 'bg-pink-50/80 border-pink-300/60 hover:bg-pink-100/80 hover:border-pink-400/80';
  };

  const handleCellClick = (month: number, expenseTypeId: number, e?: React.MouseEvent) => {
    if (!user) return;
    
    // Se estiver em modo de seleção, alterna seleção
    if (selectionMode || e?.ctrlKey || e?.metaKey) {
      const cellKey = `${month}-${expenseTypeId}`;
      setSelectedCells((prev) => {
        const newSet = new Set(prev);
        if (newSet.has(cellKey)) {
          newSet.delete(cellKey);
        } else {
          newSet.add(cellKey);
        }
        return newSet;
      });
      return;
    }
    
    // Se não estiver em modo de seleção, edita normalmente
    const value = getCellValue(month, expenseTypeId);
    setEditingCell({ month, expenseTypeId });
    setEditValue(value > 0 ? value.toString() : '');
    setTimeout(() => inputRef.current?.focus(), 0);
  };

  const handleDeleteSelected = async () => {
    if (selectedCells.size === 0) return;
    
    if (!confirm(`Deseja excluir ${selectedCells.size} despesa(s) selecionada(s)?`)) {
      return;
    }

    try {
      const deletePromises: Promise<void>[] = [];
      
      selectedCells.forEach((cellKey) => {
        const [month, expenseTypeId] = cellKey.split('-').map(Number);
        const expense = expenses.find(
          (e) => e.month === month && e.expenseTypeId === expenseTypeId
        );
        if (expense) {
          deletePromises.push(expenseService.deleteExpense(expense.id));
        }
      });

      await Promise.all(deletePromises);
      await loadData();
      setSelectedCells(new Set());
      setSelectionMode(false);
    } catch (error) {
      console.error('Error deleting expenses:', error);
      alert('Erro ao excluir despesas');
    }
  };

  const clearSelection = () => {
    setSelectedCells(new Set());
    setSelectionMode(false);
  };

  const handleSave = async (month: number, expenseTypeId: number, value: string) => {
    if (!user) return;
    
    const trimmedValue = value.trim();
    const numValue = parseFloat(trimmedValue.replace(',', '.'));
    
    // Se o valor for vazio ou 0, deleta a despesa se existir
    if (!trimmedValue || isNaN(numValue) || numValue <= 0) {
      const existingExpense = expenses.find(
        (e) => e.month === month && e.expenseTypeId === expenseTypeId
      );
      
      if (existingExpense) {
        try {
          await expenseService.deleteExpense(existingExpense.id);
          await loadData();
        } catch (error) {
          console.error('Error deleting expense:', error);
          alert('Erro ao excluir despesa');
        }
      }
      setEditingCell(null);
      return;
    }

    try {
      const request: ExpenseRequest = {
        userId: user.id,
        expenseTypeId,
        amount: numValue,
        month,
        year,
      };
      await expenseService.createOrUpdateExpense(request);
      await loadData();
      setEditingCell(null);
    } catch (error) {
      console.error('Error saving expense:', error);
      alert('Erro ao salvar despesa');
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent, month: number, expenseTypeId: number) => {
    if (e.key === 'Enter') {
      handleSave(month, expenseTypeId, editValue);
    } else if (e.key === 'Escape') {
      setEditingCell(null);
    }
  };

  const handleBlur = (month: number, expenseTypeId: number) => {
    handleSave(month, expenseTypeId, editValue);
  };

  const calculateMonthTotal = (month: number): number => {
    return expenses
      .filter((e) => e.month === month)
      .reduce((sum, e) => sum + e.amount, 0);
  };

  const calculateTypeTotal = (expenseTypeId: number): number => {
    return expenses
      .filter((e) => e.expenseTypeId === expenseTypeId)
      .reduce((sum, e) => sum + e.amount, 0);
  };

  const calculateGrandTotal = (): number => {
    return expenses.reduce((sum, e) => sum + e.amount, 0);
  };

  const calculateLucasDebt = (month: number): number => {
    const monthExpenses = expenses.filter((e) => e.month === month);
    
    // Total do mês (todas as despesas)
    const totalMonth = monthExpenses.reduce((sum, e) => sum + e.amount, 0);
    
    // Cada um deveria pagar (metade do total) - lógica Splitwise
    const eachShouldPay = totalMonth / 2;
    
    // O que cada um pagou
    const lucasPaid = monthExpenses
      .filter((e) => e.userColor === 'blue')
      .reduce((sum, e) => sum + e.amount, 0);
    
    const marianaPaid = monthExpenses
      .filter((e) => e.userColor === 'pink')
      .reduce((sum, e) => sum + e.amount, 0);
    
    // Saldo do Lucas: o que ele pagou - o que ele deveria pagar
    // Se negativo: Lucas deve para Mariana
    // Se positivo: Mariana deve para Lucas
    const lucasBalance = lucasPaid - eachShouldPay;
    
    // Retornar o valor que Lucas deve (negativo do saldo se ele deve)
    // Se lucasBalance é negativo, ele deve o valor absoluto
    // Se lucasBalance é positivo, Mariana deve (retornar negativo para indicar)
    return -lucasBalance;
  };

  // Função para calcular dívida do mês atual (não utilizada atualmente)
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const calculateCurrentMonthLucasDebt = (): number => {
    const currentMonth = new Date().getMonth() + 1;
    return calculateLucasDebt(currentMonth);
  };

  const formatCurrency = (value: number): string => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-64">
        <div className="flex flex-col items-center gap-3">
          <svg className="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <div className="text-gray-600 font-medium">Carregando...</div>
        </div>
      </div>
    );
  }

  const lucasDebt = calculateLucasDebt(selectedMonth);
  const annualTotal = calculateGrandTotal();

  const fullMonthNames = [
    'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
    'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
  ];

  return (
    <div className="space-y-6">
      {/* Botão Flutuante de Alertas */}
      {alerts && alerts.alerts.length > 0 && (
        <button
          onClick={() => setShowAlerts(true)}
          className="fixed bottom-6 right-6 z-40 w-16 h-16 bg-gradient-to-br from-orange-500 to-red-600 text-white rounded-full shadow-2xl hover:shadow-orange-500/50 hover:scale-110 transition-all flex items-center justify-center group"
          title="Ver Alertas Inteligentes"
        >
          <div className="relative">
            <span className="text-3xl animate-pulse">🚨</span>
            {alerts.summary.totalAlerts > 0 && (
              <span className="absolute -top-2 -right-2 bg-red-600 text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center border-2 border-white">
                {alerts.summary.totalAlerts}
              </span>
            )}
          </div>
        </button>
      )}

      {/* Modal de Alertas */}
      {alerts && alerts.alerts.length > 0 && showAlerts && (
        <>
          {/* Overlay */}
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity"
            onClick={() => setShowAlerts(false)}
          />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="bg-white rounded-2xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className={`flex items-center justify-between p-6 border-b-2 ${
                alerts.summary.overallStatus === 'critical' ? 'bg-gradient-to-r from-red-50 to-orange-50 border-red-200' :
                alerts.summary.overallStatus === 'attention' ? 'bg-gradient-to-r from-yellow-50 to-orange-50 border-yellow-200' :
                'bg-gradient-to-r from-green-50 to-emerald-50 border-green-200'
              }`}>
                <div className="flex items-center gap-3">
                  <div className={`w-12 h-12 rounded-xl flex items-center justify-center shadow-md ${
                    alerts.summary.overallStatus === 'critical' ? 'bg-gradient-to-br from-red-500 to-red-600' :
                    alerts.summary.overallStatus === 'attention' ? 'bg-gradient-to-br from-yellow-500 to-orange-500' :
                    'bg-gradient-to-br from-green-500 to-green-600'
                  }`}>
                    <span className="text-3xl">
                      {alerts.summary.overallStatus === 'critical' ? '🚨' :
                       alerts.summary.overallStatus === 'attention' ? '⚠️' : '✅'}
                    </span>
                  </div>
                  <div>
                    <h2 className="text-xl font-bold text-slate-800">Alertas Inteligentes</h2>
                    <p className="text-sm text-slate-600">
                      {alerts.summary.totalAlerts} alerta{alerts.summary.totalAlerts !== 1 ? 's' : ''} detectado{alerts.summary.totalAlerts !== 1 ? 's' : ''} para {fullMonthNames[selectedMonth - 1]} {year}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowAlerts(false)}
                  className="text-slate-500 hover:text-slate-700 p-2 hover:bg-white/50 rounded-lg transition-colors"
                  title="Fechar"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Content */}
              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {/* Resumo */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-gradient-to-br from-slate-50 to-slate-100 rounded-xl p-4 border border-slate-200">
                    <p className="text-xs font-semibold text-slate-600 uppercase mb-1">Total do Mês</p>
                    <p className="text-2xl font-bold text-slate-800">
                      {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(alerts.summary.totalMonthSpent)}
                    </p>
                  </div>
                  <div className="bg-gradient-to-br from-blue-50 to-blue-100 rounded-xl p-4 border border-blue-200">
                    <p className="text-xs font-semibold text-blue-600 uppercase mb-1">Média Histórica</p>
                    <p className="text-2xl font-bold text-blue-800">
                      {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(alerts.summary.averageMonthSpent)}
                    </p>
                  </div>
                  <div className={`rounded-xl p-4 border ${
                    alerts.summary.totalMonthSpent > alerts.summary.averageMonthSpent
                      ? 'bg-gradient-to-br from-red-50 to-orange-50 border-red-200'
                      : 'bg-gradient-to-br from-green-50 to-emerald-50 border-green-200'
                  }`}>
                    <p className="text-xs font-semibold text-slate-600 uppercase mb-1">Diferença</p>
                    <p className={`text-2xl font-bold ${
                      alerts.summary.totalMonthSpent > alerts.summary.averageMonthSpent ? 'text-red-600' : 'text-green-600'
                    }`}>
                      {alerts.summary.totalMonthSpent > alerts.summary.averageMonthSpent ? '+' : ''}
                      {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(
                        alerts.summary.totalMonthSpent - alerts.summary.averageMonthSpent
                      )}
                    </p>
                  </div>
                </div>

                {/* Lista de Alertas */}
                <div className="space-y-4">
                  {alerts.alerts.map((alert, idx) => (
                    <div
                      key={idx}
                      className={`bg-white rounded-xl p-5 border-2 shadow-sm hover:shadow-md transition-all ${
                        alert.severity === 'critical' ? 'border-red-300 bg-red-50/30' :
                        alert.severity === 'warning' ? 'border-yellow-300 bg-yellow-50/30' :
                        'border-slate-200 bg-slate-50/30'
                      }`}
                    >
                      <div className="flex items-start gap-4">
                        <span className="text-4xl flex-shrink-0">{alert.icon}</span>
                        <div className="flex-1 min-w-0">
                          <div className="flex flex-wrap items-center gap-2 mb-2">
                            <h3 className="font-bold text-lg text-slate-900">{alert.expenseTypeName}</h3>
                            <span className={`text-xs font-semibold px-3 py-1 rounded-full ${
                              alert.severity === 'critical' ? 'bg-red-500 text-white' :
                              alert.severity === 'warning' ? 'bg-yellow-500 text-white' :
                              'bg-slate-400 text-white'
                            }`}>
                              {alert.severity === 'critical' ? 'CRÍTICO' :
                               alert.severity === 'warning' ? 'ATENÇÃO' : 'INFO'}
                            </span>
                            {alert.isHistoricalMax && (
                              <span className="text-xs font-semibold px-3 py-1 rounded-full bg-purple-500 text-white">
                                RECORDE
                              </span>
                            )}
                          </div>
                          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-3">
                            <div className="bg-white/70 rounded-lg p-2 border border-red-200">
                              <span className="text-xs text-slate-600 block">Atual:</span>
                              <span className="font-bold text-red-600 text-lg">
                                {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(alert.currentValue)}
                              </span>
                            </div>
                            <div className="bg-white/70 rounded-lg p-2 border border-slate-200">
                              <span className="text-xs text-slate-600 block">Média:</span>
                              <span className="font-semibold text-slate-700 text-lg">
                                {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(alert.averageValue)}
                              </span>
                            </div>
                            <div className="bg-white/70 rounded-lg p-2 border border-orange-200">
                              <span className="text-xs text-slate-600 block">Variação:</span>
                              <span className={`font-bold text-lg ${alert.percentageAboveAverage > 0 ? 'text-red-600' : 'text-green-600'}`}>
                                {alert.percentageAboveAverage > 0 ? '+' : ''}{alert.percentageAboveAverage.toFixed(1)}%
                              </span>
                            </div>
                          </div>
                          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                            <p className="text-sm text-blue-900 font-medium">💡 {alert.suggestion}</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div className="bg-gradient-to-br from-blue-50 via-blue-50/50 to-blue-100/80 p-6 rounded-2xl shadow-md border-2 border-blue-200/60 hover:shadow-lg transition-shadow backdrop-blur-sm">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <p className="text-sm font-semibold text-blue-800 mb-2 uppercase tracking-wide">Dívida do Lucas</p>
              <p className={`text-3xl font-bold mb-1 ${
                lucasDebt > 0 ? 'text-blue-700' : lucasDebt < 0 ? 'text-emerald-600' : 'text-slate-600'
              }`}>
                {lucasDebt > 0 
                  ? formatCurrency(lucasDebt) + ' (Lucas deve)'
                  : lucasDebt < 0
                  ? formatCurrency(Math.abs(lucasDebt)) + ' (Mariana deve)'
                  : formatCurrency(0) + ' (Quites)'
                }
              </p>
              <p className="text-sm font-medium text-blue-700/80 mt-3 mb-1">
                Total do mês: <span className="font-bold">{formatCurrency(calculateMonthTotal(selectedMonth))}</span>
              </p>
            </div>
            <div className="flex flex-col items-end gap-2 ml-4 mt-1">
              <select
                value={selectedMonth}
                onChange={(e) => setSelectedMonth(Number(e.target.value))}
                className="px-3 py-1.5 text-xs border-2 border-blue-300/60 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400/50 focus:border-blue-400 transition-all bg-white/90 shadow-sm hover:border-blue-400 font-medium text-blue-700 backdrop-blur-sm"
              >
                {fullMonthNames.map((month, index) => (
                  <option key={index + 1} value={index + 1}>
                    {month} {year}
                  </option>
                ))}
              </select>
              <div className="text-4xl opacity-80">💰</div>
            </div>
          </div>
        </div>

            <div className="bg-gradient-to-br from-red-50 via-red-50/50 to-red-100/80 p-6 rounded-2xl shadow-md border-2 border-red-200/60 hover:shadow-lg transition-shadow backdrop-blur-sm">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold text-red-800 mb-1 uppercase tracking-wide">Total Anual</p>
                  <p className="text-xs text-red-600/80 mb-3 font-medium">
                    Todas as despesas de {year}
                  </p>
                  <p className="text-3xl font-bold text-red-700 mb-1">
                    {formatCurrency(annualTotal)}
                  </p>
                  <p className="text-xs font-medium text-red-600/80 mt-2 flex items-center gap-1">
                    <span className="w-1.5 h-1.5 bg-red-500 rounded-full"></span>
                    {expenses.length} despesa{expenses.length !== 1 ? 's' : ''} registrada{expenses.length !== 1 ? 's' : ''}
                  </p>
                </div>
                <div className="text-4xl opacity-80">📊</div>
              </div>
            </div>
      </div>

      {/* Botão Gerar Análise IA */}
      <div className="flex justify-center mb-6">
        <button
          onClick={handleGenerateAiAnalysis}
          disabled={loadingAiAnalysis}
          className="px-6 py-3 bg-gradient-to-r from-purple-500 to-indigo-600 text-white font-bold rounded-xl shadow-lg hover:shadow-xl transition-all transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
        >
          {loadingAiAnalysis ? (
            <>
              <div className="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full" />
              Gerando Análise...
            </>
          ) : (
            <>
              <span className="text-2xl">🤖</span>
              Gerar Análise IA Completa
            </>
          )}
        </button>
      </div>

      {/* Card de Insights IA */}
      {aiAnalysis && (
        <div className="bg-gradient-to-br from-purple-50 via-indigo-50 to-blue-50 p-6 rounded-2xl shadow-md border-2 border-purple-200/60 hover:shadow-lg transition-shadow mb-6">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <div className="flex items-center gap-2 mb-3">
                <span className="text-2xl">🤖</span>
                <p className="text-sm font-semibold text-purple-800 uppercase tracking-wide">
                  Análise Inteligente
                </p>
              </div>

              {/* Score de Saúde Financeira */}
              <div className="mb-4">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-xs text-purple-600 font-medium">Saúde Financeira</span>
                  <span className="text-lg font-bold text-purple-700">
                    {aiAnalysis.financialHealthScore}/100
                  </span>
                </div>
                <div className="w-full bg-purple-200 rounded-full h-2">
                  <div
                    className={`h-2 rounded-full transition-all ${
                      aiAnalysis.financialHealthScore >= 70 ? 'bg-green-500' :
                      aiAnalysis.financialHealthScore >= 40 ? 'bg-yellow-500' :
                      'bg-red-500'
                    }`}
                    style={{ width: `${aiAnalysis.financialHealthScore}%` }}
                  />
                </div>
              </div>

              {/* Resumo Executivo */}
              <p className="text-sm text-purple-900 mb-3">
                {aiAnalysis.executiveSummary}
              </p>

              {/* Previsão Próximo Mês */}
              {aiAnalysis.nextMonthPrediction && (
                <div className="bg-white/50 rounded-lg p-3 mb-3">
                  <p className="text-xs font-semibold text-purple-600 mb-1">
                    📊 Previsão Próximo Mês
                  </p>
                  <p className="text-lg font-bold text-purple-800">
                    {formatCurrency(aiAnalysis.nextMonthPrediction.predictedAmount)}
                  </p>
                  <p className="text-xs text-purple-600 mt-1">
                    Confiança: {(aiAnalysis.nextMonthPrediction.confidence * 100).toFixed(0)}%
                  </p>
                </div>
              )}

              {/* Botão Ver Detalhes */}
              <button
                onClick={() => setShowAiAnalysisModal(true)}
                className="text-sm font-semibold text-purple-700 hover:text-purple-900 underline"
              >
                Ver Análise Completa →
              </button>
            </div>

            <div className="text-4xl opacity-80 ml-4">🎯</div>
          </div>
        </div>
      )}

      <div className="bg-white/90 backdrop-blur-sm rounded-2xl shadow-md overflow-hidden border-2 border-slate-200/60">
        <div className="flex justify-between items-center bg-slate-50/80 p-4 border-b-2 border-slate-200/60">
          <div className="flex items-center gap-4">
            <h2 className="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">Planilha de Despesas</h2>
            <select
              value={year}
              onChange={(e) => {
                setYear(Number(e.target.value));
                clearSelection();
              }}
              className="px-3 py-1.5 text-sm border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-400/50 focus:border-blue-400 transition-all bg-white font-medium text-slate-700 shadow-sm hover:border-slate-400"
            >
              {Array.from({ length: 10 }, (_, i) => 2023 + i).map((y) => (
                <option key={y} value={y}>
                  {y}
                </option>
              ))}
            </select>
          </div>
              <div className="flex items-center gap-3">
                <RecurringExpenseButton
                  expenseTypes={expenseTypes}
                  year={year}
                  onCreate={handleCreateRecurringExpense}
                />
            <button
              onClick={() => {
                setSelectionMode(!selectionMode);
                if (selectionMode) {
                  clearSelection();
                }
              }}
              className={`px-4 py-2 text-sm font-semibold rounded-xl transition-all shadow-sm ${
                selectionMode
                  ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-md ring-2 ring-blue-400/30'
                  : 'bg-slate-100 text-slate-700 hover:bg-slate-200 border-2 border-slate-300 hover:border-slate-400'
              }`}
            >
              {selectionMode ? 'Cancelar Seleção' : 'Excluir Células'}
            </button>
            {selectedCells.size > 0 && (
              <>
                <span className="text-sm text-slate-600 font-semibold px-3 py-1.5 bg-slate-100 rounded-lg border-2 border-slate-300">
                  {selectedCells.size} selecionada(s)
                </span>
                <button
                  onClick={handleDeleteSelected}
                  className="px-4 py-2 text-sm font-semibold text-white bg-gradient-to-r from-red-500 to-red-600 rounded-xl hover:from-red-600 hover:to-red-700 transition-all shadow-md hover:shadow-lg ring-2 ring-red-400/30"
                >
                  Excluir Selecionadas
                </button>
              </>
            )}
          </div>
        </div>
        <div className="overflow-x-auto">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gradient-to-r from-slate-50 via-slate-50 to-slate-100/80">
              <tr>
                <th className="px-1 py-2 text-left text-[10px] font-bold text-slate-700 uppercase tracking-wider sticky left-0 bg-gradient-to-r from-slate-50 via-slate-50 to-slate-100/80 z-10 border-r-2 border-slate-200/60 w-[80px] shadow-sm">
                  <div className="flex items-center justify-between gap-1">
                    <span>Tipo</span>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setShowAddTypeModal(true);
                      }}
                      className="text-emerald-600 hover:text-emerald-700 hover:bg-emerald-100 rounded p-0.5 transition-colors"
                      title="Adicionar tipo"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                      </svg>
                    </button>
                  </div>
                </th>
                {monthNames.map((month, index) => {
                  const monthNumber = index + 1;
                  const monthKey = `${year}-${monthNumber}`;
                  const isPaid = paidMonths.has(monthKey);
                  
                  return (
                    <th
                      key={index}
                      onClick={() => {
                        const newPaidMonths = new Set(paidMonths);
                        if (isPaid) {
                          newPaidMonths.delete(monthKey);
                        } else {
                          newPaidMonths.add(monthKey);
                        }
                        setPaidMonths(newPaidMonths);
                        localStorage.setItem(`paidMonths_${year}`, JSON.stringify(Array.from(newPaidMonths)));
                      }}
                      className={`px-2 py-2 text-center text-[10px] font-bold uppercase tracking-wider border-r border-slate-200/60 last:border-r-0 cursor-pointer transition-all ${
                        isPaid 
                          ? 'bg-emerald-500 text-white hover:bg-emerald-600' 
                          : 'bg-gradient-to-r from-slate-50 via-slate-50 to-slate-100/80 text-slate-700 hover:bg-slate-200/60'
                      }`}
                      title={isPaid ? 'Mês pago - Clique para desmarcar' : 'Clique para marcar como pago'}
                    >
                      {month}
                    </th>
                  );
                })}
                <th className="px-3 py-2 text-center text-[10px] font-bold text-slate-700 uppercase tracking-wider bg-slate-200/60">
                  Total
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-slate-100/60">
              {expenseTypes.map((type) => (
                <tr key={type.id} className="hover:bg-slate-50/50 transition-colors">
                  <td className="px-1 py-1.5 text-[10px] font-semibold text-slate-800 sticky left-0 bg-white z-10 border-r-2 border-slate-200/60 shadow-sm w-[80px] group/type">
                    <div className="flex items-center justify-between gap-1">
                      <span className="flex-1 truncate">{type.name}</span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleDeleteExpenseType(type.id, type.name);
                        }}
                        className="opacity-0 group-hover/type:opacity-100 text-red-600 hover:text-red-700 hover:bg-red-50 rounded p-0.5 transition-all"
                        title="Excluir tipo"
                      >
                        <svg xmlns="http://www.w3.org/2000/svg" className="h-3 w-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </button>
                    </div>
                  </td>
                  {monthNames.map((_, monthIndex) => {
                    const month = monthIndex + 1;
                    const monthKey = `${year}-${month}`;
                    const isMonthPaid = paidMonths.has(monthKey);
                    const value = getCellValue(month, type.id);
                    const colorClass = getCellColor(month, type.id);
                    const isEditing = editingCell?.month === month && editingCell?.expenseTypeId === type.id;
                    const cellKey = `${month}-${type.id}`;
                    const isSelected = selectedCells.has(cellKey);
                    const existingExpense = expenses.find(
                      (e) => e.month === month && e.expenseTypeId === type.id
                    );
                    
                    return (
                      <td
                        key={monthIndex}
                        className={`px-2 py-1.5 text-[11px] text-center border border-r border-slate-200/40 ${
                          isMonthPaid 
                            ? 'bg-emerald-50/80 border-emerald-200/60 hover:bg-emerald-100/80' 
                            : colorClass
                        } ${
                          isEditing ? '' : 'cursor-pointer hover:opacity-90'
                        } ${
                          isSelected ? 'ring-2 ring-amber-400/60 bg-amber-50/80' : ''
                        } transition-all relative group`}
                        onClick={(e) => !isEditing && handleCellClick(month, type.id, e)}
                      >
                        {isEditing ? (
                          <input
                            ref={inputRef}
                            type="text"
                            value={editValue}
                            onChange={(e) => setEditValue(e.target.value)}
                            onBlur={() => handleBlur(month, type.id)}
                            onKeyDown={(e) => handleKeyDown(e, month, type.id)}
                            className="w-full text-center text-[11px] font-medium bg-white border-2 border-blue-500 rounded-lg px-1 py-0.5 focus:outline-none focus:ring-2 focus:ring-blue-400/50 shadow-sm"
                            autoFocus
                            placeholder="0.00"
                          />
                        ) : (
                          <>
                            {value > 0 ? (
                              <span className="font-medium">{formatCurrency(value)}</span>
                            ) : (
                              <span className="text-slate-300">-</span>
                            )}
                            {existingExpense && (
                              <button
                                onClick={(e) => {
                                  e.stopPropagation();
                                  if (confirm('Deseja excluir esta despesa?')) {
                                    expenseService.deleteExpense(existingExpense.id).then(() => {
                                      loadData();
                                    }).catch((error) => {
                                      console.error('Error deleting expense:', error);
                                      alert('Erro ao excluir despesa');
                                    });
                                  }
                                }}
                                className="absolute top-0.5 right-0.5 opacity-0 group-hover:opacity-100 transition-opacity bg-gradient-to-br from-red-500 to-red-600 hover:from-red-600 hover:to-red-700 text-white rounded-full w-4 h-4 flex items-center justify-center text-[10px] font-bold shadow-md ring-1 ring-red-400/30"
                                title="Excluir despesa"
                              >
                                ×
                              </button>
                            )}
                          </>
                        )}
                      </td>
                    );
                  })}
                  <td className="px-3 py-1.5 text-xs font-bold text-center bg-gray-50 border-l-2 border-gray-300">
                    {formatCurrency(calculateTypeTotal(type.id))}
                  </td>
                </tr>
              ))}
              <tr className="bg-gradient-to-r from-slate-100 via-slate-100 to-slate-200/80 font-bold border-t-2 border-slate-300/60">
                <td className="px-1 py-2 text-[10px] sticky left-0 bg-gradient-to-r from-slate-100 via-slate-100 to-slate-200/80 z-10 border-r-2 border-slate-300/60 shadow-sm w-[80px] text-slate-700">
                  Total
                </td>
                {monthNames.map((_, monthIndex) => {
                  const month = monthIndex + 1;
                  const monthKey = `${year}-${month}`;
                  const isMonthPaid = paidMonths.has(monthKey);
                  
                  return (
                    <td
                      key={monthIndex}
                      className={`px-2 py-2 text-[11px] text-center border-r border-slate-200/60 font-semibold ${
                        isMonthPaid 
                          ? 'bg-emerald-100/80 text-emerald-800' 
                          : 'text-slate-700'
                      }`}
                    >
                      {formatCurrency(calculateMonthTotal(month))}
                    </td>
                  );
                })}
                <td className="px-3 py-2 text-xs text-center bg-slate-200/80 font-extrabold border-l-2 border-slate-400/60 text-slate-800">
                  {formatCurrency(calculateGrandTotal())}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal para adicionar tipo */}
      {showAddTypeModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-2xl shadow-xl w-full max-w-md p-6 space-y-4 border-2 border-slate-200">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-bold text-slate-800">Adicionar Tipo de Despesa</h3>
              <button
                onClick={() => {
                  setShowAddTypeModal(false);
                  setNewTypeName('');
                }}
                className="text-slate-400 hover:text-slate-600 transition-colors"
              >
                <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            <div>
              <label htmlFor="type-name" className="block text-sm font-semibold text-slate-700 mb-2">
                Nome do Tipo
              </label>
              <input
                id="type-name"
                type="text"
                value={newTypeName}
                onChange={(e) => setNewTypeName(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddExpenseType();
                  } else if (e.key === 'Escape') {
                    setShowAddTypeModal(false);
                    setNewTypeName('');
                  }
                }}
                placeholder="Ex: Aluguel, Viagem, Dívida Carro..."
                className="w-full px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 transition-all bg-white text-slate-800"
                autoFocus
              />
            </div>

            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setShowAddTypeModal(false);
                  setNewTypeName('');
                }}
                className="px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 border-2 border-slate-300 rounded-lg hover:bg-slate-200 transition-all"
              >
                Cancelar
              </button>
              <button
                onClick={handleAddExpenseType}
                className="px-4 py-2 text-sm font-semibold text-white bg-gradient-to-r from-emerald-500 to-emerald-600 rounded-lg hover:from-emerald-600 hover:to-emerald-700 transition-all shadow-md"
              >
                Adicionar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal de Análise IA Completa */}
      {showAiAnalysisModal && aiAnalysis && (
        <>
          {/* Overlay */}
          <div
            className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity"
            onClick={() => setShowAiAnalysisModal(false)}
          />

          {/* Modal */}
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div
              className="bg-white rounded-2xl shadow-2xl max-w-7xl w-full max-h-[95vh] overflow-hidden flex flex-col"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Header */}
              <div className="flex items-center justify-between p-6 border-b-2 border-purple-200/50 bg-gradient-to-r from-purple-50 to-indigo-50">
                <div className="flex items-center gap-3">
                  <span className="text-3xl">🤖</span>
                  <div>
                    <h2 className="text-xl font-bold text-slate-800">Análise Inteligente Completa</h2>
                    <p className="text-sm text-slate-600">
                      {fullMonthNames[selectedMonth - 1]} {year}
                    </p>
                  </div>
                </div>
                <button
                  onClick={() => setShowAiAnalysisModal(false)}
                  className="text-slate-500 hover:text-slate-700 p-2 hover:bg-white/50 rounded-lg transition-colors"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>

              {/* Content */}
              <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {/* ⚡ DECISÃO RÁPIDA - Quick Stats Section */}
                <div className="bg-gradient-to-br from-purple-100 via-indigo-100 to-blue-100 rounded-xl p-5 border-2 border-purple-300 shadow-lg">
                  <div className="flex items-center gap-2 mb-4">
                    <span className="text-2xl">⚡</span>
                    <h3 className="text-lg font-bold text-purple-900">Decisão Rápida</h3>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">
                    <div
                      className="flex items-center gap-2 cursor-help relative group"
                      title="Score de 0-100 que avalia sua situação financeira considerando gastos, padrões e tendências. 70+: Boa | 40-69: Atenção | 0-39: Crítica"
                    >
                      <span className={`text-xl ${
                        aiAnalysis.financialHealthScore >= 70 ? '✅' :
                        aiAnalysis.financialHealthScore >= 40 ? '⚠️' : '❌'
                      }`}></span>
                      <span className="font-semibold text-gray-700">Saúde Financeira:</span>
                      <span className={`font-bold ${
                        aiAnalysis.financialHealthScore >= 70 ? 'text-green-700' :
                        aiAnalysis.financialHealthScore >= 40 ? 'text-yellow-700' : 'text-red-700'
                      }`}>
                        {aiAnalysis.financialHealthScore >= 70 ? 'BOA' :
                         aiAnalysis.financialHealthScore >= 40 ? 'ATENÇÃO' : 'CRÍTICA'} ({aiAnalysis.financialHealthScore}/100)
                      </span>
                      <div className="absolute top-full left-0 mt-2 hidden group-hover:block w-72 p-3 bg-gray-900 text-white text-xs rounded-lg shadow-xl z-50 pointer-events-none">
                        <div className="absolute bottom-full left-4 w-0 h-0 border-l-8 border-r-8 border-b-8 border-transparent border-b-gray-900"></div>
                        <div className="space-y-2">
                          <div>
                            <strong className="text-green-300">Saúde Financeira:</strong> Score de 0-100 que avalia sua situação financeira considerando gastos, padrões e tendências.
                          </div>
                          <div className="pl-2 space-y-1 text-gray-300">
                            <div>• <span className="text-green-400">70-100:</span> Boa gestão financeira</div>
                            <div>• <span className="text-yellow-400">40-69:</span> Requer atenção</div>
                            <div>• <span className="text-red-400">0-39:</span> Situação crítica</div>
                          </div>
                        </div>
                      </div>
                    </div>

                    {aiAnalysis.economicContext?.ipca && (
                      <div className="flex items-center gap-2 cursor-help relative group">
                        <span className="text-xl">💰</span>
                        <span
                          className="font-semibold text-gray-700"
                          title="Índice de Preços ao Consumidor Amplo - Mede a inflação oficial do Brasil calculada pelo IBGE"
                        >
                          IPCA:
                        </span>
                        <span className="font-bold text-red-600">{aiAnalysis.economicContext.ipca.value.toFixed(2)}%</span>
                        {aiAnalysis.economicContext.igpm && (
                          <>
                            <span className="text-gray-500">|</span>
                            <span
                              className="font-semibold text-gray-700"
                              title="Índice Geral de Preços do Mercado - Inflação que mede variação de preços no atacado, construção civil e consumidor"
                            >
                              IGP-M:
                            </span>
                            <span className="font-bold text-orange-600">{aiAnalysis.economicContext.igpm.value.toFixed(2)}%</span>
                          </>
                        )}
                        <div className="absolute top-full left-0 mt-2 hidden group-hover:block w-80 p-3 bg-gray-900 text-white text-xs rounded-lg shadow-xl z-50 pointer-events-none">
                          <div className="absolute bottom-full left-4 w-0 h-0 border-l-8 border-r-8 border-b-8 border-transparent border-b-gray-900"></div>
                          <div className="space-y-2">
                            <div>
                              <strong className="text-yellow-300">IPCA:</strong> Inflação oficial do Brasil medida pelo IBGE. Mede o custo de vida de famílias com renda de 1 a 40 salários mínimos.
                            </div>
                            <div>
                              <strong className="text-orange-300">IGP-M:</strong> Índice da FGV que mede inflação do atacado (60%), consumidor (30%) e construção civil (10%). Usado em contratos de aluguel.
                            </div>
                          </div>
                        </div>
                      </div>
                    )}

                    {aiAnalysis.nextMonthPrediction && (
                      <div className="flex items-center gap-2">
                        <span className="text-xl">🎯</span>
                        <span className="font-semibold text-gray-700">Predição Próx:</span>
                        <span className="font-bold text-blue-700">
                          {formatCurrency(aiAnalysis.nextMonthPrediction.predictedAmount)}
                          <span className="text-xs ml-1">({(aiAnalysis.nextMonthPrediction.confidence * 100).toFixed(0)}%)</span>
                        </span>
                      </div>
                    )}

                    {aiAnalysis.economicContext?.usdBrl && (
                      <div className="flex items-center gap-2">
                        <span className="text-xl">💵</span>
                        <span className="font-semibold text-gray-700">Dólar:</span>
                        <span className="font-bold text-gray-700">
                          R$ {aiAnalysis.economicContext.usdBrl.value.toFixed(2)}
                          <span className={`text-xs ml-1 ${aiAnalysis.economicContext.usdBrl.variation >= 0 ? 'text-red-600' : 'text-green-600'}`}>
                            ({aiAnalysis.economicContext.usdBrl.variation >= 0 ? '+' : ''}{aiAnalysis.economicContext.usdBrl.variation.toFixed(2)}%)
                          </span>
                        </span>
                      </div>
                    )}
                  </div>
                </div>

                {/* 🎯 VISÃO GERAL - Health Score Gauge */}
                <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm">
                  <div className="flex items-center gap-2 mb-4">
                    <span className="text-2xl">🎯</span>
                    <h3 className="text-lg font-bold text-slate-800">Visão Geral</h3>
                  </div>
                  <HealthScoreGauge score={aiAnalysis.financialHealthScore} />
                  <div className="mt-4 p-3 bg-slate-50 rounded-lg">
                    <p className="text-sm text-slate-700">{aiAnalysis.executiveSummary}</p>
                  </div>
                </div>

                {/* 💰 ANÁLISE DE RENDA DA CASA */}
                {aiAnalysis.householdIncome && (
                  <div className="bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-5 border border-green-200 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                      <span className="text-2xl">💰</span>
                      <h3 className="text-lg font-bold text-green-900">Análise de Renda da Casa</h3>
                    </div>

                    {/* Quick Summary Cards */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
                      {/* Renda Total */}
                      <div className="bg-white rounded-lg p-3 text-center border border-green-200">
                        <div className="text-2xl mb-1">💵</div>
                        <div className="text-xs font-semibold text-gray-600 mb-1">Renda Total</div>
                        <div className="text-lg font-bold text-green-600">
                          {formatCurrency(aiAnalysis.householdIncome.totalHouseholdIncome)}
                        </div>
                      </div>

                      {/* Gastos Totais */}
                      <div className="bg-white rounded-lg p-3 text-center border border-red-200">
                        <div className="text-2xl mb-1">📊</div>
                        <div className="text-xs font-semibold text-gray-600 mb-1">Gastos Totais</div>
                        <div className="text-lg font-bold text-red-600">
                          {formatCurrency(aiAnalysis.householdIncome.totalExpenses)}
                        </div>
                      </div>

                      {/* Poupança */}
                      <div className="bg-white rounded-lg p-3 text-center border border-blue-200">
                        <div className="text-2xl mb-1">
                          {aiAnalysis.householdIncome.savings >= 0 ? '💰' : '⚠️'}
                        </div>
                        <div className="text-xs font-semibold text-gray-600 mb-1">Poupança</div>
                        <div className={`text-lg font-bold ${
                          aiAnalysis.householdIncome.savings >= 0 ? 'text-green-600' : 'text-red-600'
                        }`}>
                          {formatCurrency(aiAnalysis.householdIncome.savings)}
                        </div>
                      </div>

                      {/* Taxa de Poupança */}
                      <div className="bg-white rounded-lg p-3 text-center border border-purple-200">
                        <div className="text-2xl mb-1">📈</div>
                        <div className="text-xs font-semibold text-gray-600 mb-1">Taxa Poupança</div>
                        <div className={`text-lg font-bold ${
                          aiAnalysis.householdIncome.savingsRate >= 20 ? 'text-green-600' :
                          aiAnalysis.householdIncome.savingsRate >= 10 ? 'text-yellow-600' : 'text-red-600'
                        }`}>
                          {aiAnalysis.householdIncome.savingsRate.toFixed(1)}%
                        </div>
                      </div>
                    </div>

                    {/* Grid 2 Colunas - Gauge e Renda vs Gastos */}
                    <div className="grid grid-cols-1 lg:grid-cols-2 gap-5 mb-5">
                      {/* Savings Rate Gauge */}
                      <div>
                        <h4 className="text-sm font-semibold text-green-800 mb-3">Capacidade de Poupança</h4>
                        <SavingsRateGauge rate={aiAnalysis.householdIncome.savingsRate} />
                      </div>

                      {/* Income vs Expenses Chart */}
                      <div>
                        <h4 className="text-sm font-semibold text-green-800 mb-3">Renda vs Gastos</h4>
                        <IncomeVsExpensesChart data={aiAnalysis.householdIncome} />
                      </div>
                    </div>

                    {/* Income Stability Chart - Largura Total */}
                    <div className="mb-5">
                      <h4 className="text-sm font-semibold text-green-800 mb-3">Estabilidade de Renda (6 Meses)</h4>
                      <IncomeStabilityChart historicalData={aiAnalysis.householdIncome.historicalData} />
                      <div className="mt-2 p-3 bg-white rounded-lg border border-green-200">
                        <p className="text-sm text-gray-700">
                          <span className="font-semibold">Score de Estabilidade:</span>{' '}
                          <span className={`font-bold ${
                            aiAnalysis.householdIncome.incomeStabilityScore >= 70 ? 'text-green-600' :
                            aiAnalysis.householdIncome.incomeStabilityScore >= 40 ? 'text-yellow-600' : 'text-red-600'
                          }`}>
                            {aiAnalysis.householdIncome.incomeStabilityScore}/100 - {aiAnalysis.householdIncome.incomeStabilityStatus}
                          </span>
                        </p>
                      </div>
                    </div>

                    {/* Budget Status Alert */}
                    <div className={`p-4 rounded-lg border-l-4 ${
                      aiAnalysis.householdIncome.budgetStatus === 'Excelente' ? 'bg-green-50 border-green-500' :
                      aiAnalysis.householdIncome.budgetStatus === 'Bom' ? 'bg-blue-50 border-blue-500' :
                      aiAnalysis.householdIncome.budgetStatus === 'Atenção' ? 'bg-yellow-50 border-yellow-500' :
                      'bg-red-50 border-red-500'
                    }`}>
                      <p className="font-bold text-sm mb-2">
                        Status do Orçamento: {aiAnalysis.householdIncome.budgetStatus}
                      </p>
                      {aiAnalysis.householdIncome.recommendations && aiAnalysis.householdIncome.recommendations.length > 0 && (
                        <ul className="text-sm space-y-1">
                          {aiAnalysis.householdIncome.recommendations.map((rec, idx) => (
                            <li key={idx}>• {rec}</li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                )}

                {/* 📈 EVOLUÇÃO DOS GASTOS */}
                {aiAnalysis.historicalData && aiAnalysis.historicalData.length > 0 && (
                  <div className="bg-white rounded-xl p-5 border border-slate-200 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                      <span className="text-2xl">📈</span>
                      <h3 className="text-lg font-bold text-slate-800">Evolução dos Gastos (6 Meses)</h3>
                    </div>
                    <SpendingTrendChart data={aiAnalysis.historicalData} />
                  </div>
                )}

                {/* 💰 CONTEXTO ECONÔMICO */}
                {aiAnalysis.economicContext && (
                  <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-5 border border-blue-200 shadow-sm">
                    <div className="flex items-center gap-2 mb-4">
                      <span className="text-2xl">💰</span>
                      <h3 className="text-lg font-bold text-blue-900">Contexto Econômico</h3>
                    </div>

                    {/* Cards Grid */}
                    <div className="grid grid-cols-2 md:grid-cols-4 gap-3 mb-5">
                      {aiAnalysis.economicContext.ipca && (
                        <div className="bg-white rounded-lg p-3 text-center border border-red-200">
                          <div className="text-2xl mb-1">📈</div>
                          <div className="text-xs font-semibold text-gray-600 mb-1">IPCA</div>
                          <div className="text-lg font-bold text-red-600">{aiAnalysis.economicContext.ipca.value.toFixed(2)}%</div>
                        </div>
                      )}
                      {aiAnalysis.economicContext.igpm && (
                        <div className="bg-white rounded-lg p-3 text-center border border-orange-200">
                          <div className="text-2xl mb-1">📉</div>
                          <div className="text-xs font-semibold text-gray-600 mb-1">IGP-M</div>
                          <div className="text-lg font-bold text-orange-600">{aiAnalysis.economicContext.igpm.value.toFixed(2)}%</div>
                        </div>
                      )}
                      {aiAnalysis.economicContext.selic && (
                        <div className="bg-white rounded-lg p-3 text-center border border-purple-200">
                          <div className="text-2xl mb-1">💰</div>
                          <div className="text-xs font-semibold text-gray-600 mb-1">Selic</div>
                          <div className="text-lg font-bold text-purple-600">{aiAnalysis.economicContext.selic.value.toFixed(2)}%</div>
                        </div>
                      )}
                      {aiAnalysis.economicContext.usdBrl && (
                        <div className="bg-white rounded-lg p-3 text-center border border-green-200">
                          <div className="text-2xl mb-1">💵</div>
                          <div className="text-xs font-semibold text-gray-600 mb-1">Dólar</div>
                          <div className="text-lg font-bold text-green-600">R$ {aiAnalysis.economicContext.usdBrl.value.toFixed(2)}</div>
                        </div>
                      )}
                    </div>

                    {/* Gráfico de Comparação com Inflação */}
                    {aiAnalysis.historicalData && aiAnalysis.historicalData.length > 0 && (
                      <div>
                        <h4 className="text-sm font-semibold text-blue-800 mb-3">Gastos vs Inflação</h4>
                        <InflationComparisonChart historicalData={aiAnalysis.historicalData} />
                      </div>
                    )}
                  </div>
                )}

                {/* Comparação */}
                {aiAnalysis.comparison && (
                  <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
                    <p className="text-sm font-semibold text-slate-700 mb-3">📊 Comparações</p>
                    <div className="space-y-2">
                      <div className="flex items-start gap-2">
                        <span className="text-sm font-medium text-slate-600 min-w-[120px]">vs Mês Anterior:</span>
                        <span className="text-sm text-slate-700">{aiAnalysis.comparison.vsLastMonth}</span>
                      </div>
                      <div className="flex items-start gap-2">
                        <span className="text-sm font-medium text-slate-600 min-w-[120px]">vs Média:</span>
                        <span className="text-sm text-slate-700">{aiAnalysis.comparison.vsAverage}</span>
                      </div>
                      <div className="flex items-start gap-2">
                        <span className="text-sm font-medium text-slate-600 min-w-[120px]">Tendência:</span>
                        <span className={`text-sm font-semibold ${
                          aiAnalysis.comparison.trend === 'increasing' ? 'text-red-600' :
                          aiAnalysis.comparison.trend === 'decreasing' ? 'text-green-600' :
                          'text-slate-600'
                        }`}>
                          {aiAnalysis.comparison.trend === 'increasing' ? '📈 Crescente' :
                           aiAnalysis.comparison.trend === 'decreasing' ? '📉 Decrescente' :
                           '➡️ Estável'}
                        </span>
                      </div>
                    </div>
                  </div>
                )}

                {/* Padrões Detectados */}
                {aiAnalysis.patternsDetected && aiAnalysis.patternsDetected.length > 0 && (
                  <div className="bg-white rounded-xl p-4 border border-slate-200 shadow-sm">
                    <p className="text-sm font-semibold text-slate-700 mb-3">🔍 Padrões Identificados</p>
                    <div className="space-y-3">
                      {aiAnalysis.patternsDetected.map((pattern, idx) => (
                        <div key={idx} className="flex items-start gap-3 p-3 bg-slate-50 rounded-lg">
                          <span className="text-2xl">{pattern.icon}</span>
                          <div className="flex-1">
                            <p className="text-sm font-semibold text-slate-800 mb-1">
                              {pattern.description}
                            </p>
                            <p className="text-xs text-slate-600">{pattern.insight}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Previsão Próximo Mês */}
                {aiAnalysis.nextMonthPrediction && (
                  <div className="bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl p-4 border border-blue-200">
                    <p className="text-sm font-semibold text-blue-800 mb-3">🔮 Previsão para o Próximo Mês</p>
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-blue-700">Valor Previsto:</span>
                        <span className="text-2xl font-bold text-blue-800">
                          {formatCurrency(aiAnalysis.nextMonthPrediction.predictedAmount)}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-sm text-blue-700">Confiança:</span>
                        <span className="text-sm font-semibold text-blue-800">
                          {(aiAnalysis.nextMonthPrediction.confidence * 100).toFixed(0)}%
                        </span>
                      </div>
                      <div className="bg-white/50 rounded-lg p-3">
                        <p className="text-xs font-semibold text-blue-700 mb-1">Raciocínio:</p>
                        <p className="text-sm text-blue-900">{aiAnalysis.nextMonthPrediction.reasoning}</p>
                      </div>
                      {aiAnalysis.nextMonthPrediction.assumptions && aiAnalysis.nextMonthPrediction.assumptions.length > 0 && (
                        <div>
                          <p className="text-xs font-semibold text-blue-700 mb-2">Premissas:</p>
                          <ul className="list-disc list-inside space-y-1">
                            {aiAnalysis.nextMonthPrediction.assumptions.map((assumption, idx) => (
                              <li key={idx} className="text-xs text-blue-800">{assumption}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  </div>
                )}

                {/* Recomendações */}
                {aiAnalysis.recommendations && aiAnalysis.recommendations.length > 0 && (
                  <div className="bg-gradient-to-br from-green-50 to-emerald-50 rounded-xl p-4 border border-green-200">
                    <p className="text-sm font-semibold text-green-800 mb-3">💡 Recomendações</p>
                    <ul className="space-y-2">
                      {aiAnalysis.recommendations.map((rec, idx) => (
                        <li key={idx} className="flex items-start gap-2">
                          <span className="text-green-600 mt-0.5">✓</span>
                          <span className="text-sm text-green-900">{rec}</span>
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  );
};

export default ExpenseSheet;

