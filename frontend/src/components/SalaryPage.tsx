import { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { salaryService } from '../services/salaryService';
import { salaryDeductionService } from '../services/salaryDeductionService';
import type { Salary, SalaryRequest, SalaryCalculationResponse, AnnualSalaryCalculationResponse, SalaryDeduction, BoletoProcessResponse } from '../types';
import type { ExtractUploadRequest } from '../services/extractService';

const SalaryPage = () => {
  const { user } = useAuth();
  const [salary, setSalary] = useState<Salary | null>(null);
  const [loading, setLoading] = useState(false);
  const [fixedAmount, setFixedAmount] = useState<string>('');
  const [hourlyRate, setHourlyRate] = useState<string>('');
  const [selectedMonth, setSelectedMonth] = useState<number>(new Date().getMonth() + 1);
  const [selectedYear, setSelectedYear] = useState<number>(new Date().getFullYear());
  const [calculation, setCalculation] = useState<SalaryCalculationResponse | null>(null);
  const [annualCalculation, setAnnualCalculation] = useState<AnnualSalaryCalculationResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [deductions, setDeductions] = useState<SalaryDeduction[]>([]);
  const [loadingDeductions, setLoadingDeductions] = useState(false);
  const [showBoletoModal, setShowBoletoModal] = useState(false);
  const [boletoFile, setBoletoFile] = useState<File | null>(null);
  const [processingBoleto, setProcessingBoleto] = useState(false);
  const [boletoData, setBoletoData] = useState<BoletoProcessResponse | null>(null);
  const [editingBoleto, setEditingBoleto] = useState<{ description: string; amount: string; dueDate: string }>({ description: '', amount: '', dueDate: '' });

  const isLucas = user?.email === 'vyeiralucas@gmail.com';
  const isMariana = user?.email === 'marii_borges@hotmail.com';

  useEffect(() => {
    if (user) {
      loadSalary();
    }
  }, [user]);

  useEffect(() => {
    if (isLucas && salary?.hourlyRate && selectedMonth && selectedYear) {
      calculateSalary();
      calculateAnnualSalary();
      loadDeductions();
    }
  }, [isLucas, salary?.hourlyRate, selectedMonth, selectedYear]);

  useEffect(() => {
    if (isLucas && user && selectedMonth && selectedYear) {
      loadDeductions();
    }
  }, [isLucas, user, selectedMonth, selectedYear]);

  const loadSalary = async () => {
    if (!user) return;
    setLoading(true);
    try {
      const data = await salaryService.getSalaryByUser(user.id);
      setSalary(data);
      if (data) {
        if (data.fixedAmount) {
          setFixedAmount(data.fixedAmount.toString());
        }
        if (data.hourlyRate) {
          setHourlyRate(data.hourlyRate.toString());
        }
      } else {
        setFixedAmount('');
        setHourlyRate('');
      }
    } catch (err) {
      console.error('Erro ao carregar salário:', err);
      setError('Erro ao carregar salário.');
    } finally {
      setLoading(false);
    }
  };

  const calculateSalary = async () => {
    if (!user || !salary?.hourlyRate) return;
    try {
      const result = await salaryService.calculateVariableSalary({
        userId: user.id,
        month: selectedMonth,
        year: selectedYear,
      });
      console.log('Resultado do cálculo:', result);
      setCalculation(result);
    } catch (err) {
      console.error('Erro ao calcular salário:', err);
    }
  };

  const calculateAnnualSalary = async () => {
    if (!user || !salary?.hourlyRate) return;
    try {
      const result = await salaryService.calculateAnnualSalary(user.id, selectedYear);
      console.log('Resultado do cálculo anual:', result);
      setAnnualCalculation(result);
    } catch (err) {
      console.error('Erro ao calcular salário anual:', err);
    }
  };

  const loadDeductions = async () => {
    if (!user || !isLucas) return;
    setLoadingDeductions(true);
    try {
      const data = await salaryDeductionService.getDeductions(user.id, selectedMonth, selectedYear);
      setDeductions(data);
    } catch (err) {
      console.error('Erro ao carregar boletos:', err);
    } finally {
      setLoadingDeductions(false);
    }
  };

  const convertFileToBase64 = (file: File): Promise<string> => {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onload = () => {
        const base64 = (reader.result as string).split(',')[1];
        resolve(base64);
      };
      reader.onerror = reject;
      reader.readAsDataURL(file);
    });
  };

  const handleBoletoFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const fileType = file.type;
    if (fileType !== 'application/pdf' && !fileType.startsWith('image/')) {
      setError('Por favor, selecione um arquivo PDF ou imagem PNG/JPG');
      return;
    }

    setBoletoFile(file);
    setProcessingBoleto(true);
    setError(null);

    try {
      const fileTypeStr = fileType === 'application/pdf' ? 'pdf' : 'png';
      const base64Content = await convertFileToBase64(file);

      const request: ExtractUploadRequest = {
        fileName: file.name,
        fileContent: base64Content,
        fileType: fileTypeStr as 'pdf' | 'png',
      };

      const response = await salaryDeductionService.processBoleto(request);
      
      if (response.error) {
        setError(response.error);
        setBoletoFile(null);
        return;
      }

      setBoletoData(response);
      setEditingBoleto({
        description: response.description || '',
        amount: response.amount?.toString() || '',
        dueDate: response.dueDate || '',
      });
    } catch (err: any) {
      setError(err.response?.data?.error || 'Erro ao processar boleto. Verifique se a API key do OpenAI está configurada.');
      setBoletoFile(null);
    } finally {
      setProcessingBoleto(false);
    }
  };

  const handleSaveBoleto = async () => {
    if (!user || !isLucas) return;

    if (!editingBoleto.description || !editingBoleto.amount || !editingBoleto.dueDate) {
      setError('Por favor, preencha todos os campos.');
      return;
    }

    try {
      await salaryDeductionService.createDeduction({
        userId: user.id,
        description: editingBoleto.description,
        amount: parseFloat(editingBoleto.amount),
        dueDate: editingBoleto.dueDate,
        month: selectedMonth,
        year: selectedYear,
      });

      setShowBoletoModal(false);
      setBoletoFile(null);
      setBoletoData(null);
      setEditingBoleto({ description: '', amount: '', dueDate: '' });
      await loadDeductions();
      await calculateSalary();
      await calculateAnnualSalary();
    } catch (err) {
      console.error('Erro ao salvar boleto:', err);
      setError('Erro ao salvar boleto.');
    }
  };

  const handleDeleteDeduction = async (id: number) => {
    if (!confirm('Deseja realmente excluir este boleto?')) return;
    
    try {
      await salaryDeductionService.deleteDeduction(id);
      await loadDeductions();
      await calculateSalary();
      await calculateAnnualSalary();
    } catch (err) {
      console.error('Erro ao excluir boleto:', err);
      setError('Erro ao excluir boleto.');
    }
  };

  const handleSave = async () => {
    if (!user) return;

    if (isMariana && (!fixedAmount || parseFloat(fixedAmount) <= 0)) {
      setError('Por favor, informe um valor válido.');
      return;
    }

    if (isLucas && (!hourlyRate || parseFloat(hourlyRate) <= 0)) {
      setError('Por favor, informe um valor por hora válido.');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const request: SalaryRequest = {
        userId: user.id,
      };

      if (isMariana) {
        request.fixedAmount = parseFloat(fixedAmount);
        request.currency = 'BRL';
      }

      if (isLucas) {
        request.hourlyRate = parseFloat(hourlyRate);
        request.currency = 'USD';
      }

      const saved = await salaryService.createOrUpdateSalary(request);
      setSalary(saved);
      setError(null);
    } catch (err) {
      console.error('Erro ao salvar salário:', err);
      setError('Erro ao salvar salário.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!salary) return;
    if (!confirm('Deseja realmente excluir seu salário?')) return;

    setLoading(true);
    try {
      await salaryService.deleteSalary(salary.id);
      setSalary(null);
      setFixedAmount('');
      setHourlyRate('');
      setCalculation(null);
    } catch (err) {
      console.error('Erro ao excluir salário:', err);
      setError('Erro ao excluir salário.');
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (value: number, currency: string = 'BRL'): string => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
  };

  const getMonthName = (month: number): string => {
    const months = [
      'Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho',
      'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro'
    ];
    return months[month - 1];
  };

  const years = Array.from({ length: 10 }, (_, i) => new Date().getFullYear() - 2 + i);

  return (
    <div className="max-w-4xl mx-auto p-6 space-y-6">
      <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6">
        <h2 className="text-2xl font-bold text-slate-800 mb-6">
          {isMariana ? 'Salário Fixo' : isLucas ? 'Salário Variável (por hora)' : 'Salário'}
        </h2>

        {error && (
          <div className="bg-red-50 border-2 border-red-200 text-red-700 px-4 py-3 rounded-lg mb-4">
            {error}
          </div>
        )}

        {/* Formulário para Mariana (Fixo) */}
        {isMariana && (
          <>
            <div className="bg-slate-50 border-2 border-slate-200 rounded-xl p-6 mb-6">
              <h3 className="text-lg font-semibold text-slate-800 mb-4">
                {salary?.fixedAmount ? 'Atualizar Salário Fixo' : 'Registrar Salário Fixo'}
              </h3>
              
              <div className="flex items-end gap-4">
                <div className="flex-1">
                  <label className="block text-sm font-semibold text-slate-700 mb-2">
                    Valor Mensal (R$)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={fixedAmount}
                    onChange={(e) => setFixedAmount(e.target.value)}
                    placeholder="0.00"
                    className="w-full px-4 py-3 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 transition-all bg-white text-slate-800 text-lg"
                  />
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={handleSave}
                    disabled={loading || !fixedAmount}
                    className="px-6 py-3 bg-gradient-to-r from-emerald-600 to-emerald-700 text-white rounded-lg hover:from-emerald-700 hover:to-emerald-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold shadow-md hover:shadow-lg"
                  >
                    {loading ? 'Salvando...' : salary?.fixedAmount ? 'Atualizar' : 'Salvar'}
                  </button>
                  {salary?.fixedAmount && (
                    <button
                      onClick={handleDelete}
                      disabled={loading}
                      className="px-4 py-3 border-2 border-red-300 text-red-700 rounded-lg hover:bg-red-50 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Excluir
                    </button>
                  )}
                </div>
              </div>
            </div>

            {/* Exibição do Salário Fixo */}
            {salary?.fixedAmount && (
              <div className="bg-gradient-to-r from-emerald-50 to-emerald-100 border-2 border-emerald-200 rounded-xl p-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-bold text-slate-800 mb-1">
                      Salário Fixo Registrado
                    </h3>
                    <p className="text-sm text-slate-600">
                      Última atualização: {new Date(salary.updatedAt).toLocaleDateString('pt-BR')}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-3xl font-bold text-emerald-700">
                      {formatCurrency(salary.fixedAmount, 'BRL')}
                    </p>
                    <p className="text-sm text-slate-600 mt-1">por mês</p>
                  </div>
                </div>
              </div>
            )}
          </>
        )}

        {/* Formulário para Lucas (Variável) */}
        {isLucas && (
          <>
            <div className="bg-slate-50 border-2 border-slate-200 rounded-xl p-6 mb-6">
              <h3 className="text-lg font-semibold text-slate-800 mb-4">
                {salary?.hourlyRate ? 'Atualizar Valor por Hora' : 'Registrar Valor por Hora'}
              </h3>
              
              <div className="flex items-end gap-4 mb-4">
                <div className="flex-1">
                  <label className="block text-sm font-semibold text-slate-700 mb-2">
                    Valor por Hora (USD)
                  </label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={hourlyRate}
                    onChange={(e) => setHourlyRate(e.target.value)}
                    placeholder="0.00"
                    className="w-full px-4 py-3 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 transition-all bg-white text-slate-800 text-lg"
                  />
                </div>

                <div className="flex gap-2">
                  <button
                    onClick={handleSave}
                    disabled={loading || !hourlyRate}
                    className="px-6 py-3 bg-gradient-to-r from-emerald-600 to-emerald-700 text-white rounded-lg hover:from-emerald-700 hover:to-emerald-800 transition-all disabled:opacity-50 disabled:cursor-not-allowed font-semibold shadow-md hover:shadow-lg"
                  >
                    {loading ? 'Salvando...' : salary?.hourlyRate ? 'Atualizar' : 'Salvar'}
                  </button>
                  {salary?.hourlyRate && (
                    <button
                      onClick={handleDelete}
                      disabled={loading}
                      className="px-4 py-3 border-2 border-red-300 text-red-700 rounded-lg hover:bg-red-50 transition-colors font-semibold disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                      Excluir
                    </button>
                  )}
                </div>
              </div>

              {/* Seletores de Mês/Ano para Cálculo */}
              {salary?.hourlyRate && (
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-2">
                      Mês
                    </label>
                    <select
                      value={selectedMonth}
                      onChange={(e) => setSelectedMonth(parseInt(e.target.value, 10))}
                      className="w-full px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all bg-white text-slate-800"
                    >
                      <option value={1}>Janeiro</option>
                      <option value={2}>Fevereiro</option>
                      <option value={3}>Março</option>
                      <option value={4}>Abril</option>
                      <option value={5}>Maio</option>
                      <option value={6}>Junho</option>
                      <option value={7}>Julho</option>
                      <option value={8}>Agosto</option>
                      <option value={9}>Setembro</option>
                      <option value={10}>Outubro</option>
                      <option value={11}>Novembro</option>
                      <option value={12}>Dezembro</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-2">
                      Ano
                    </label>
                    <select
                      value={selectedYear}
                      onChange={(e) => setSelectedYear(parseInt(e.target.value, 10))}
                      className="w-full px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-all bg-white text-slate-800"
                    >
                      {years.map((year) => (
                        <option key={year} value={year}>
                          {year}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              )}
            </div>

            {/* Exibição do Valor por Hora Registrado */}
            {salary?.hourlyRate && (
              <div className="bg-gradient-to-r from-blue-50 to-blue-100 border-2 border-blue-200 rounded-xl p-6 mb-6">
                <div className="flex items-center justify-between">
                  <div>
                    <h3 className="text-lg font-bold text-slate-800 mb-1">
                      Valor por Hora Registrado
                    </h3>
                    <p className="text-sm text-slate-600">
                      Última atualização: {new Date(salary.updatedAt).toLocaleDateString('pt-BR')}
                    </p>
                  </div>
                  <div className="text-right">
                    <p className="text-3xl font-bold text-blue-700">
                      {formatCurrency(salary.hourlyRate, 'USD')}
                    </p>
                    <p className="text-sm text-slate-600 mt-1">por hora</p>
                  </div>
                </div>
              </div>
            )}

            {/* Cálculo Anual */}
            {annualCalculation && (
              <div className="bg-gradient-to-r from-blue-50 to-blue-100 border-2 border-blue-200 rounded-xl p-6 mb-6">
                <h3 className="text-lg font-bold text-slate-800 mb-4">
                  Projeção Anual {annualCalculation.year}
                </h3>
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <p className="text-sm text-slate-600">Total de dias úteis</p>
                    <p className="text-xl font-bold text-slate-800">{annualCalculation.totalWorkingDays} dias</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600">Total de horas</p>
                    <p className="text-xl font-bold text-slate-800">{annualCalculation.totalHours} horas</p>
                  </div>
                </div>
                <div className="border-t-2 border-blue-300 pt-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-lg font-semibold text-slate-800">Total anual (USD)</p>
                    <p className="text-2xl font-bold text-blue-700">
                      {formatCurrency(annualCalculation.totalAmountUSD, 'USD')}
                    </p>
                  </div>
                  <div className="flex items-center justify-between bg-white/60 rounded-lg p-3">
                    <div>
                      <p className="text-sm text-slate-600">Taxa de câmbio</p>
                      <p className="text-xs text-slate-500">
                        USD → BRL: {(annualCalculation.exchangeRate || 5.42).toFixed(2)}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-semibold text-slate-800">Salário bruto anual (BRL)</p>
                      <p className="text-2xl font-bold text-emerald-700">
                        {formatCurrency(annualCalculation.totalAmountBRL || (annualCalculation.totalAmountUSD * (annualCalculation.exchangeRate || 5.42)), 'BRL')}
                      </p>
                    </div>
                  </div>
                  {annualCalculation.totalDeductions !== undefined && annualCalculation.totalDeductions > 0 && (
                    <div className="flex items-center justify-between bg-red-50 rounded-lg p-3 border-2 border-red-200">
                      <p className="text-lg font-semibold text-slate-800">Total de descontos no ano</p>
                      <p className="text-2xl font-bold text-red-700">
                        - {formatCurrency(annualCalculation.totalDeductions, 'BRL')}
                      </p>
                    </div>
                  )}
                  <div className="flex items-center justify-between bg-gradient-to-r from-emerald-100 to-emerald-200 rounded-lg p-4 border-2 border-emerald-300">
                    <p className="text-xl font-bold text-slate-800">Salário líquido anual</p>
                    <p className="text-3xl font-bold text-emerald-800">
                      {formatCurrency(
                        annualCalculation.netSalaryBRL || (annualCalculation.totalAmountBRL || (annualCalculation.totalAmountUSD * (annualCalculation.exchangeRate || 5.42))) - (annualCalculation.totalDeductions || 0),
                        'BRL'
                      )}
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Seção de Boletos (apenas para Lucas) */}
            {isLucas && (
              <div className="bg-white/80 backdrop-blur-sm rounded-2xl shadow-lg border-2 border-slate-200/60 p-6 mb-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-bold text-slate-800">
                    Boletos - {getMonthName(selectedMonth)} de {selectedYear}
                  </h3>
                  <button
                    onClick={() => setShowBoletoModal(true)}
                    className="px-4 py-2 text-sm font-semibold text-white bg-gradient-to-r from-blue-500 to-blue-600 rounded-lg hover:from-blue-600 hover:to-blue-700 transition-all shadow-md flex items-center gap-2"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    Adicionar Boleto
                  </button>
                </div>

                {loadingDeductions ? (
                  <div className="flex justify-center py-4">
                    <svg className="animate-spin h-6 w-6 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                  </div>
                ) : deductions.length > 0 ? (
                  <div className="space-y-2">
                    {deductions.map((deduction) => (
                      <div key={deduction.id} className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                        <div className="flex-1">
                          <p className="font-semibold text-slate-800">{deduction.description}</p>
                          <p className="text-sm text-slate-600">
                            Vencimento: {new Date(deduction.dueDate).toLocaleDateString('pt-BR')}
                          </p>
                        </div>
                        <div className="flex items-center gap-4">
                          <p className="text-lg font-bold text-red-700">
                            {formatCurrency(deduction.amount, 'BRL')}
                          </p>
                          <button
                            onClick={() => handleDeleteDeduction(deduction.id)}
                            className="text-red-600 hover:text-red-800 font-semibold px-2 py-1 hover:bg-red-50 rounded transition-colors"
                          >
                            Excluir
                          </button>
                        </div>
                      </div>
                    ))}
                    <div className="mt-4 pt-4 border-t-2 border-slate-300 flex items-center justify-between">
                      <p className="text-lg font-bold text-slate-800">Total de descontos:</p>
                      <p className="text-2xl font-bold text-red-700">
                        {formatCurrency(
                          deductions.reduce((sum, d) => sum + d.amount, 0),
                          'BRL'
                        )}
                      </p>
                    </div>
                  </div>
                ) : (
                  <div className="text-center py-6 text-slate-500">
                    <p>Nenhum boleto adicionado para este mês.</p>
                  </div>
                )}
              </div>
            )}

            {/* Cálculo do Salário do Mês */}
            {calculation && (
              <div className="bg-gradient-to-r from-emerald-50 to-emerald-100 border-2 border-emerald-200 rounded-xl p-6">
                <h3 className="text-lg font-bold text-slate-800 mb-4">
                  Cálculo para {getMonthName(calculation.month)} de {calculation.year}
                </h3>
                <div className="grid grid-cols-2 gap-4 mb-4">
                  <div>
                    <p className="text-sm text-slate-600">Dias úteis</p>
                    <p className="text-xl font-bold text-slate-800">{calculation.workingDays} dias</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600">Horas por dia</p>
                    <p className="text-xl font-bold text-slate-800">{calculation.hoursPerDay} horas</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600">Total de horas</p>
                    <p className="text-xl font-bold text-slate-800">{calculation.totalHours} horas</p>
                  </div>
                  <div>
                    <p className="text-sm text-slate-600">Valor por hora</p>
                    <p className="text-xl font-bold text-slate-800">
                      {formatCurrency(calculation.hourlyRate, 'USD')}
                    </p>
                  </div>
                </div>
                <div className="border-t-2 border-emerald-300 pt-4 space-y-3">
                  <div className="flex items-center justify-between">
                    <p className="text-lg font-semibold text-slate-800">Salário bruto (USD)</p>
                    <p className="text-2xl font-bold text-blue-700">
                      {formatCurrency(calculation.totalAmount, 'USD')}
                    </p>
                  </div>
                  <div className="flex items-center justify-between bg-white/60 rounded-lg p-3">
                    <div>
                      <p className="text-sm text-slate-600">Taxa de câmbio</p>
                      <p className="text-xs text-slate-500">
                        USD → BRL: {(calculation.exchangeRate || 5.42).toFixed(2)}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="text-lg font-semibold text-slate-800">Salário bruto (BRL)</p>
                      <p className="text-2xl font-bold text-emerald-700">
                        {formatCurrency(
                          calculation.totalAmountBRL || (calculation.totalAmount * (calculation.exchangeRate || 5.42)), 
                          'BRL'
                        )}
                      </p>
                    </div>
                  </div>
                  {calculation.totalDeductions !== undefined && calculation.totalDeductions > 0 && (
                    <div className="flex items-center justify-between bg-red-50 rounded-lg p-3 border-2 border-red-200">
                      <p className="text-lg font-semibold text-slate-800">Total de descontos</p>
                      <p className="text-2xl font-bold text-red-700">
                        - {formatCurrency(calculation.totalDeductions, 'BRL')}
                      </p>
                    </div>
                  )}
                  <div className="flex items-center justify-between bg-gradient-to-r from-emerald-100 to-emerald-200 rounded-lg p-4 border-2 border-emerald-300">
                    <p className="text-xl font-bold text-slate-800">Salário líquido</p>
                    <p className="text-4xl font-bold text-emerald-800">
                      {formatCurrency(
                        calculation.netSalaryBRL || (calculation.totalAmountBRL || (calculation.totalAmount * (calculation.exchangeRate || 5.42))) - (calculation.totalDeductions || 0),
                        'BRL'
                      )}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </>
        )}

        {!salary && !loading && (
          <div className="text-center py-8">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="mx-auto h-12 w-12 text-slate-400"
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
            <h3 className="mt-2 text-lg font-medium text-slate-900">Nenhum salário registrado</h3>
            <p className="mt-1 text-sm text-slate-500">
              {isMariana 
                ? 'Registre seu salário fixo mensal acima.' 
                : isLucas 
                ? 'Registre seu valor por hora acima.' 
                : 'Registre seu salário acima.'}
            </p>
          </div>
        )}

        {loading && !salary && (
          <div className="flex justify-center items-center py-8">
            <svg className="animate-spin h-8 w-8 text-blue-600" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
          </div>
        )}

        {/* Modal de Adicionar Boleto */}
        {showBoletoModal && (
          <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50 p-4">
            <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
              <div className="p-6">
                <div className="flex items-center justify-between mb-6">
                  <h3 className="text-2xl font-bold text-slate-800">Adicionar Boleto</h3>
                  <button
                    onClick={() => {
                      setShowBoletoModal(false);
                      setBoletoFile(null);
                      setBoletoData(null);
                      setEditingBoleto({ description: '', amount: '', dueDate: '' });
                      setError(null);
                    }}
                    className="text-slate-400 hover:text-slate-600"
                  >
                    <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                    </svg>
                  </button>
                </div>

                {error && (
                  <div className="mb-4 bg-red-50 border-2 border-red-200 text-red-700 px-4 py-3 rounded-lg">
                    {error}
                  </div>
                )}

                {!boletoData ? (
                  <div>
                    <label className="block text-sm font-semibold text-slate-700 mb-2">
                      Arquivo do Boleto (PDF ou PNG)
                    </label>
                    <div className="border-2 border-dashed border-slate-300 rounded-lg p-6 text-center hover:border-blue-400 transition-colors">
                      <input
                        type="file"
                        accept=".pdf,.png,.jpg,.jpeg"
                        onChange={handleBoletoFileChange}
                        className="hidden"
                        id="boleto-upload"
                      />
                      <label
                        htmlFor="boleto-upload"
                        className="cursor-pointer flex flex-col items-center gap-2"
                      >
                        {boletoFile ? (
                          <>
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 text-blue-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                            </svg>
                            <span className="text-blue-700 font-semibold">{boletoFile.name}</span>
                          </>
                        ) : (
                          <>
                            <svg xmlns="http://www.w3.org/2000/svg" className="h-12 w-12 text-slate-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
                            </svg>
                            <span className="text-slate-600 font-medium">Clique para selecionar arquivo</span>
                            <span className="text-xs text-slate-500">PDF ou PNG até 10MB</span>
                          </>
                        )}
                      </label>
                    </div>
                    {processingBoleto && (
                      <div className="mt-4 flex items-center justify-center gap-2 text-blue-600">
                        <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        Processando boleto...
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-semibold text-slate-700 mb-2">
                        Descrição
                      </label>
                      <input
                        type="text"
                        value={editingBoleto.description}
                        onChange={(e) => setEditingBoleto({ ...editingBoleto, description: e.target.value })}
                        className="w-full px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        placeholder="Ex: IPTU, Conta de Luz, etc."
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-slate-700 mb-2">
                        Valor (BRL)
                      </label>
                      <input
                        type="number"
                        step="0.01"
                        value={editingBoleto.amount}
                        onChange={(e) => setEditingBoleto({ ...editingBoleto, amount: e.target.value })}
                        className="w-full px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                        placeholder="0.00"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-semibold text-slate-700 mb-2">
                        Data de Vencimento
                      </label>
                      <input
                        type="date"
                        value={editingBoleto.dueDate}
                        onChange={(e) => setEditingBoleto({ ...editingBoleto, dueDate: e.target.value })}
                        className="w-full px-4 py-2 border-2 border-slate-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      />
                    </div>
                    <div className="flex gap-3 pt-4">
                      <button
                        onClick={handleSaveBoleto}
                        className="flex-1 px-4 py-2 text-sm font-semibold text-white bg-gradient-to-r from-emerald-500 to-emerald-600 rounded-lg hover:from-emerald-600 hover:to-emerald-700 transition-all shadow-md"
                      >
                        Salvar Boleto
                      </button>
                      <button
                        onClick={() => {
                          setBoletoData(null);
                          setBoletoFile(null);
                          setEditingBoleto({ description: '', amount: '', dueDate: '' });
                        }}
                        className="px-4 py-2 text-sm font-medium text-slate-700 bg-slate-100 border-2 border-slate-300 rounded-lg hover:bg-slate-200 transition-all"
                      >
                        Cancelar
                      </button>
                    </div>
                  </div>
                )}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default SalaryPage;
