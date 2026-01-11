// Types for the application

export interface User {
  id: number;
  email: string;
  name: string;
  color: string;
}

export interface ExpenseType {
  id: number;
  name: string;
}

export interface Expense {
  id: number;
  userId: number;
  userName: string;
  userColor: string;
  expenseTypeId: number;
  expenseTypeName: string;
  amount: number;
  month: number;
  year: number;
  recurringExpenseId?: number | null;
  createdAt: string;
}

export interface RecurringExpense {
  id: number;
  userId: number;
  userName: string;
  userColor: string;
  expenseTypeId: number;
  expenseTypeName: string;
  monthlyAmount: number;
  startDate: string;
  endDate: string;
  createdAt: string;
}

export interface RecurringExpenseRequest {
  userId: number;
  expenseTypeId: number;
  monthlyAmount: number;
  startDate: string;
  endDate: string;
}

export interface ExpenseRequest {
  userId: number;
  expenseTypeId: number;
  amount: number;
  month: number;
  year: number;
}

export interface ApiResponse<T> {
  data?: T;
  message?: string;
  error?: string;
}

export interface Configuration {
  id?: number;
  key: string;
  value: string;
  description?: string;
}

export interface IdentifiedTransaction {
  description: string;
  amount: number;
  date: string;
  expenseTypeId: number;
  expenseTypeName: string;
  confidence: 'high' | 'medium' | 'low';
}

export interface ExtractProcessResponse {
  transactions: IdentifiedTransaction[];
  rawText: string;
  error?: string;
}

export interface ExtractTransaction {
  id: number;
  userId: number;
  userName: string;
  userColor: string;
  expenseTypeId: number;
  expenseTypeName: string;
  description: string;
  amount: number;
  transactionDate: string;
  createdAt: string;
}

export interface Salary {
  id: number;
  userId: number;
  userName: string;
  userColor: string;
  fixedAmount?: number | null; // Para salário fixo (Mariana)
  hourlyRate?: number | null; // Para salário variável (Lucas)
  currency?: string | null; // "BRL" ou "USD"
  createdAt: string;
  updatedAt: string;
}

export interface SalaryRequest {
  userId: number;
  fixedAmount?: number | null;
  hourlyRate?: number | null;
  currency?: string | null;
}

export interface SalaryCalculationRequest {
  userId: number;
  month: number;
  year: number;
}

export interface SalaryCalculationResponse {
  hourlyRate: number;
  workingDays: number;
  hoursPerDay: number;
  totalHours: number;
  totalAmount: number;
  totalAmountBRL?: number;
  totalDeductions?: number;
  lucasDebt?: number; // Dívida do Lucas para Mariana (se positivo, Lucas deve; se negativo, Mariana deve; se zero, quites)
  netSalaryBRL?: number;
  exchangeRate?: number;
  currency: string;
  month: number;
  year: number;
}

export interface AnnualSalaryCalculationResponse {
  hourlyRate: number;
  year: number;
  totalWorkingDays: number;
  hoursPerDay: number;
  totalHours: number;
  totalAmountUSD: number;
  totalAmountBRL: number;
  totalDeductions?: number;
  totalLucasDebt?: number; // Dívida total do Lucas para Mariana no ano (se positivo, Lucas deve; se negativo, Mariana deve; se zero, quites)
  netSalaryBRL?: number;
  exchangeRate: number;
  currency: string;
}

export interface BoletoProcessResponse {
  description: string;
  amount: number;
  dueDate: string; // YYYY-MM-DD
  error?: string;
}

export interface SalaryDeduction {
  id: number;
  userId: number;
  userName: string;
  description: string;
  amount: number;
  dueDate: string; // YYYY-MM-DD
  month: number;
  year: number;
  createdAt: string;
}

export interface SalaryDeductionRequest {
  userId: number;
  description: string;
  amount: number;
  dueDate: string; // YYYY-MM-DD
  month: number;
  year: number;
}

export interface SalaryConversionProcessRequest {
  text: string;
  month: number;
  year: number;
}

export interface SalaryConversionProcessResponse {
  conversionDate: string; // YYYY-MM-DD
  exchangeRate: number;
  amountUSD: number;
  vet: number;
  finalAmountBRL: number;
  error?: string;
}

export interface SalaryConversionRequest {
  userId: number;
  month: number;
  year: number;
  conversionDate: string; // YYYY-MM-DD
  exchangeRate: number;
  amountUSD: number;
  vet: number;
  finalAmountBRL: number;
}

export interface SalaryConversionResponse {
  id: number;
  userId: number;
  month: number;
  year: number;
  conversionDate: string; // YYYY-MM-DD
  exchangeRate: number;
  amountUSD: number;
  vet: number;
  finalAmountBRL: number;
}

export interface ExpenseInsightsResponse {
  totalSpent: number;
  totalTransactions: number;
  averagePerTransaction?: number;
  mostExpensiveDay?: string;
  mostActiveCategory?: string;
  categories: CategorySummary[];
  topExpenses: TopExpense[];
  aiInsights?: AIInsights;
  trends?: Trend[];
  quickStats?: QuickStat[];
}

export interface CategorySummary {
  categoryName: string;
  total: number;
  count: number;
  percentage: number;
}

export interface TopExpense {
  description: string;
  amount: number;
  categoryName: string;
  date: string;
}

export interface AIInsights {
  summary?: string;
  suggestions?: string[];
  warnings?: string[];
  analysis?: string;
}

export interface Trend {
  description: string;
  value: string;
  type: 'increase' | 'decrease' | 'stable';
}

export interface QuickStat {
  label: string;
  value: string;
  icon: string;
  color: string;
}

export interface ExpenseAlertsResponse {
  month: number;
  year: number;
  alerts: ExpenseAlert[];
  summary: AlertsSummary;
  aiAnalysis?: AIMonthlyAnalysis; // Análise AI completa do mês
}

export interface ExpenseAlert {
  severity: 'critical' | 'warning' | 'normal';
  expenseTypeName: string;
  currentValue: number;
  averageValue: number;
  maxHistoricalValue: number;
  percentageAboveAverage: number;
  isHistoricalMax: boolean;
  suggestion: string;
  icon: string;
}

export interface AlertsSummary {
  totalAlerts: number;
  criticalCount: number;
  warningCount: number;
  totalMonthSpent: number;
  averageMonthSpent: number;
  overallStatus: 'good' | 'attention' | 'critical';
}

// AI Monthly Analysis Types
export interface AIMonthlyAnalysis {
  executiveSummary: string;
  financialHealthScore: number; // 0-100
  patternsDetected: Pattern[];
  recommendations: string[];
  nextMonthPrediction: Prediction;
  comparison: Comparison;
}

export interface Pattern {
  type: 'temporal' | 'category' | 'trend' | 'anomaly';
  description: string;
  insight: string;
  icon: string;
}

export interface Prediction {
  predictedAmount: number;
  confidence: number; // 0-1
  reasoning: string;
  assumptions: string[];
}

export interface Comparison {
  vsLastMonth: string;
  vsAverage: string;
  trend: 'increasing' | 'decreasing' | 'stable';
}