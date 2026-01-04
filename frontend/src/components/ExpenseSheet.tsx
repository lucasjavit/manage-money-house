import { useState, useEffect, useRef } from 'react';
import { useAuth } from '../context/AuthContext';
import type { Expense, ExpenseType, ExpenseRequest } from '../types';
import { expenseService } from '../services/expenseService';
import { recurringExpenseService } from '../services/recurringExpenseService';
import RecurringExpenseButton from './RecurringExpenseButton';

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
    const marianaTotal = monthExpenses
      .filter((e) => e.userColor === 'pink')
      .reduce((sum, e) => sum + e.amount, 0);
    const lucasTotal = monthExpenses
      .filter((e) => e.userColor === 'blue')
      .reduce((sum, e) => sum + e.amount, 0);
    return marianaTotal - lucasTotal;
  };

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
      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        <div className="bg-gradient-to-br from-blue-50 via-blue-50/50 to-blue-100/80 p-6 rounded-2xl shadow-md border-2 border-blue-200/60 hover:shadow-lg transition-shadow backdrop-blur-sm">
          <div className="flex items-start justify-between">
            <div className="flex-1">
              <p className="text-sm font-semibold text-blue-800 mb-2 uppercase tracking-wide">Dívida do Lucas</p>
              <p className={`text-3xl font-bold mb-1 ${
                lucasDebt > 0 ? 'text-blue-700' : lucasDebt < 0 ? 'text-emerald-600' : 'text-slate-600'
              }`}>
                {lucasDebt > 0 
                  ? formatCurrency(lucasDebt)
                  : lucasDebt < 0
                  ? `-${formatCurrency(Math.abs(lucasDebt))}`
                  : formatCurrency(0)
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
                  Tipo
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
                  <td className="px-1 py-1.5 text-[10px] font-semibold text-slate-800 sticky left-0 bg-white z-10 border-r-2 border-slate-200/60 shadow-sm w-[80px]">
                    {type.name}
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
    </div>
  );
};

export default ExpenseSheet;

