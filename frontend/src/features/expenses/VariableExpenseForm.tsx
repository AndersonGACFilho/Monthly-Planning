import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import {
  createVariableExpense,
  replaceVariableExpense,
} from '../../api/generated/endpoints/variable-expense-controller/variable-expense-controller';
import type { VariableExpenseModel, VariableExpenseRequest } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyInput } from '../../shared/components/MoneyInput';
import { useSaved } from '../../shared/hooks/useSaved';

/** Payment methods of a variable expense; card purchases come from the bill (RF-44). */
const METHODS = ['PIX', 'DEBIT', 'CASH', 'BOLETO'] as const;

const schema = z.object({
  expenseDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/),
  billingPeriod: z.string().regex(/^\d{4}-\d{2}$/).or(z.literal('')),
  description: z.string().trim().min(1).max(120),
  memberId: z.number({ message: 'required' }),
  categoryId: z.number().nullable(),
  amount: z.number({ message: 'required' }),
  paymentMethod: z.enum(METHODS),
});

type ExpenseFormValues = z.infer<typeof schema>;

/** Props of {@link VariableExpenseForm}. */
export interface VariableExpenseFormProps {
  /** Expense being edited, or `undefined` to create one. */
  expense?: VariableExpenseModel;
  /** Selected billing period, used for the default date. */
  period: string;
  /** Called after saving or cancelling. */
  onDone: () => void;
}

/**
 * Form of a variable expense: date, description, member, category, amount and payment method (RF-34).
 *
 * @param props - {@link VariableExpenseFormProps}
 * @returns the form
 */
export function VariableExpenseForm({ expense, period, onDone }: VariableExpenseFormProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const today = new Date().toISOString().slice(0, 10);
  const { register, handleSubmit, control, formState } = useForm<ExpenseFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      expenseDate: expense?.expenseDate ?? (today.startsWith(period) ? today : `${period}-01`),
      billingPeriod: expense && expense.billingPeriod !== expense.expenseDate?.slice(0, 7) ? expense.billingPeriod : '',
      description: expense?.description ?? '',
      memberId: expense?.memberId,
      categoryId: expense?.categoryId ?? null,
      amount: expense?.amount,
      paymentMethod: (expense?.paymentMethod as ExpenseFormValues['paymentMethod'] | undefined) ?? 'PIX',
    },
  });
  const errors = formState.errors;

  const submit = handleSubmit(async (values) => {
    const request: VariableExpenseRequest = {
      expenseDate: values.expenseDate,
      billingPeriod: values.billingPeriod || undefined,
      description: values.description,
      memberId: values.memberId,
      categoryId: values.categoryId ?? undefined,
      amount: values.amount,
      paymentMethod: values.paymentMethod,
    };
    if (expense?.id !== undefined) await replaceVariableExpense(expense.id, request, ifMatch(expense.version));
    else await createVariableExpense(request);
    await saved();
    onDone();
  });

  return (
    <form onSubmit={submit} className="panel grid grid-cols-2 gap-3 wide:grid-cols-4" aria-label={t('variableExpense.form')}>
      <Field label={t('variableExpense.date')} htmlFor="expense-date" error={errors.expenseDate && t('validation.required')}>
        <input id="expense-date" type="date" className="input" {...register('expenseDate')} />
      </Field>
      <Field label={t('recurrence.description')} htmlFor="expense-description" error={errors.description && t('validation.required')}>
        <input id="expense-description" className="input" {...register('description')} />
      </Field>
      <Field label={t('recurrence.member')} htmlFor="expense-member" error={errors.memberId && t('validation.required')}>
        <Controller
          control={control}
          name="memberId"
          render={({ field }) => <MemberPicker id="expense-member" value={field.value} onChange={(v) => field.onChange(v ?? undefined)} />}
        />
      </Field>
      <Field label={t('recurrence.amount')} htmlFor="expense-amount" error={errors.amount && t('validation.required')}>
        <Controller
          control={control}
          name="amount"
          render={({ field }) => (
            <MoneyInput id="expense-amount" allowNegative value={field.value} onChange={(v) => field.onChange(v ?? undefined)} />
          )}
        />
      </Field>
      <Field label={t('recurrence.category')} htmlFor="expense-category">
        <Controller
          control={control}
          name="categoryId"
          render={({ field }) => <CategoryPicker id="expense-category" allowEmpty value={field.value} onChange={field.onChange} />}
        />
      </Field>
      <Field label={t('recurrence.paymentMethod')} htmlFor="expense-payment">
        <select id="expense-payment" className="input" {...register('paymentMethod')}>
          {METHODS.map((method) => (
            <option key={method} value={method}>
              {t(`recurrence.paymentMethods.${method}`)}
            </option>
          ))}
        </select>
      </Field>
      <Field label={t('variableExpense.billingPeriod')} htmlFor="expense-period" error={errors.billingPeriod && t('validation.period')}>
        <input id="expense-period" type="month" className="input" {...register('billingPeriod')} />
      </Field>
      <div className="col-span-full flex justify-end gap-2">
        <button type="button" className="button" onClick={onDone}>
          {t('action.cancel')}
        </button>
        <button type="submit" className="button button-primary" disabled={formState.isSubmitting}>
          {t('action.save')}
        </button>
      </div>
    </form>
  );
}
