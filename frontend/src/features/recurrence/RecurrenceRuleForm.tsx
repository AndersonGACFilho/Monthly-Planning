import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';
import {
  createRecurrenceRule,
  replaceRecurrenceRule,
} from '../../api/generated/endpoints/recurrence-rule-controller/recurrence-rule-controller';
import type { RecurrenceRuleModel, RecurrenceRuleRequest } from '../../api/generated/model';
import { ifMatch } from '../../api/requestOptions';
import { CategoryPicker } from '../../shared/components/CategoryPicker';
import { Field } from '../../shared/components/Field';
import { MemberPicker } from '../../shared/components/MemberPicker';
import { MoneyInput } from '../../shared/components/MoneyInput';
import { useSaved } from '../../shared/hooks/useSaved';

/** Rule kind: incomes and fixed expenses share this form (SPEC section 13). */
export type RuleKind = 'INCOME' | 'FIXED_EXPENSE';

const PERIOD = /^\d{4}-\d{2}$/;
const INCOME_TYPES = ['SALARY', 'FREELANCE', 'SCHOLARSHIP', 'RENT', 'OTHER'] as const;
const PAYMENT_METHODS = ['BOLETO', 'DEBIT', 'PIX', 'CASH', 'CARD'] as const;
const RECURRENCES = ['MONTHLY', 'ONCE', 'EVERY_N_MONTHS'] as const;

const schema = z
  .object({
    description: z.string().trim().min(1).max(120),
    memberId: z.number({ message: 'required' }),
    categoryId: z.number().nullable(),
    incomeType: z.enum(INCOME_TYPES),
    paymentMethod: z.enum(PAYMENT_METHODS),
    defaultAmount: z.number({ message: 'required' }).min(0),
    recurrence: z.enum(RECURRENCES),
    intervalMonths: z.number().int().min(1).max(24),
    day: z.number().int().min(1).max(31),
    validFrom: z.string().regex(PERIOD),
    validUntil: z.string().regex(PERIOD).or(z.literal('')),
  })
  .refine((v) => v.recurrence !== 'EVERY_N_MONTHS' || v.intervalMonths >= 2, { path: ['intervalMonths'] })
  .refine((v) => !v.validUntil || v.validUntil >= v.validFrom, { path: ['validUntil'] });

type RuleFormValues = z.infer<typeof schema>;

/** Props of {@link RecurrenceRuleForm}. */
export interface RecurrenceRuleFormProps {
  /** Income or fixed expense. */
  kind: RuleKind;
  /** Rule being edited, or `undefined` to create one. */
  rule?: RecurrenceRuleModel;
  /** Billing period used as the start of a new rule. */
  period: string;
  /** Starts as a one-off income in the period (RF-23). */
  oneOff?: boolean;
  /** Called after saving or cancelling. */
  onDone: () => void;
}

/**
 * One form for incomes (RF-21, RF-23) and fixed expenses (RF-30): description, member, amount, recurrence,
 * day and validity; incomes add their type, fixed expenses add category and payment method (IMP-30).
 *
 * @param props - {@link RecurrenceRuleFormProps}
 * @returns the form
 */
export function RecurrenceRuleForm({ kind, rule, period, oneOff, onDone }: RecurrenceRuleFormProps) {
  const { t } = useTranslation();
  const saved = useSaved();
  const income = kind === 'INCOME';
  const { register, handleSubmit, control, formState } = useForm<RuleFormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      description: rule?.description ?? '',
      memberId: rule?.memberId,
      categoryId: rule?.categoryId ?? null,
      incomeType: rule?.incomeType ?? 'SALARY',
      paymentMethod: rule?.paymentMethod ?? 'BOLETO',
      defaultAmount: rule?.defaultAmount,
      recurrence: rule?.recurrence ?? (oneOff ? 'ONCE' : 'MONTHLY'),
      intervalMonths: rule?.intervalMonths && rule.intervalMonths > 1 ? rule.intervalMonths : 2,
      day: rule?.day ?? (income ? 5 : 10),
      validFrom: rule?.validFrom ?? period,
      validUntil: rule?.validUntil ?? '',
    },
  });
  const errors = formState.errors;
  const recurrence = useWatch({ control, name: 'recurrence' });
  const prefix = `rule-${kind.toLowerCase()}`;

  const submit = handleSubmit(async (values) => {
    const request: RecurrenceRuleRequest = {
      description: values.description,
      memberId: values.memberId,
      categoryId: income ? undefined : (values.categoryId ?? undefined),
      incomeType: income ? values.incomeType : undefined,
      paymentMethod: income ? undefined : values.paymentMethod,
      defaultAmount: values.defaultAmount,
      recurrence: values.recurrence,
      intervalMonths: values.recurrence === 'EVERY_N_MONTHS' ? values.intervalMonths : 1,
      day: values.day,
      validFrom: values.validFrom,
      validUntil: values.recurrence === 'ONCE' || !values.validUntil ? undefined : values.validUntil,
    };
    if (rule?.id !== undefined) await replaceRecurrenceRule(rule.id, request, ifMatch(rule.version));
    else await createRecurrenceRule(request, { kind });
    await saved();
    onDone();
  });

  return (
    <form
      onSubmit={submit}
      className="panel grid grid-cols-2 gap-3 wide:grid-cols-4"
      aria-label={income ? t('income.form') : t('fixedExpense.form')}
    >
      <Field label={t('recurrence.description')} htmlFor={`${prefix}-description`} error={errors.description && t('validation.required')}>
        <input id={`${prefix}-description`} className="input" {...register('description')} />
      </Field>
      <Field label={t('recurrence.member')} htmlFor={`${prefix}-member`} error={errors.memberId && t('validation.required')}>
        <Controller
          control={control}
          name="memberId"
          render={({ field }) => <MemberPicker id={`${prefix}-member`} value={field.value} onChange={(v) => field.onChange(v ?? undefined)} />}
        />
      </Field>
      <Field label={t('recurrence.defaultAmount')} htmlFor={`${prefix}-amount`} error={errors.defaultAmount && t('validation.required')}>
        <Controller
          control={control}
          name="defaultAmount"
          render={({ field }) => <MoneyInput id={`${prefix}-amount`} value={field.value} onChange={(v) => field.onChange(v ?? undefined)} />}
        />
      </Field>
      {income ? (
        <Field label={t('recurrence.incomeType')} htmlFor={`${prefix}-type`}>
          <select id={`${prefix}-type`} className="input" {...register('incomeType')}>
            {INCOME_TYPES.map((type) => (
              <option key={type} value={type}>
                {t(`recurrence.incomeTypes.${type}`)}
              </option>
            ))}
          </select>
        </Field>
      ) : (
        <>
          <Field label={t('recurrence.category')} htmlFor={`${prefix}-category`}>
            <Controller
              control={control}
              name="categoryId"
              render={({ field }) => <CategoryPicker id={`${prefix}-category`} allowEmpty value={field.value} onChange={field.onChange} />}
            />
          </Field>
          <Field label={t('recurrence.paymentMethod')} htmlFor={`${prefix}-payment`}>
            <select id={`${prefix}-payment`} className="input" {...register('paymentMethod')}>
              {PAYMENT_METHODS.map((method) => (
                <option key={method} value={method}>
                  {t(`recurrence.paymentMethods.${method}`)}
                </option>
              ))}
            </select>
          </Field>
        </>
      )}
      <Field label={t('recurrence.recurrence')} htmlFor={`${prefix}-recurrence`}>
        <select id={`${prefix}-recurrence`} className="input" {...register('recurrence')}>
          {RECURRENCES.map((value) => (
            <option key={value} value={value}>
              {t(`recurrence.recurrences.${value}`)}
            </option>
          ))}
        </select>
      </Field>
      {recurrence === 'EVERY_N_MONTHS' && (
        <Field label={t('recurrence.intervalMonths')} htmlFor={`${prefix}-interval`} error={errors.intervalMonths && t('validation.interval')}>
          <input id={`${prefix}-interval`} type="number" min={2} max={24} className="input" {...register('intervalMonths', { valueAsNumber: true })} />
        </Field>
      )}
      <Field
        label={income ? t('recurrence.expectedDay') : t('recurrence.dueDay')}
        htmlFor={`${prefix}-day`}
        error={errors.day && t('validation.day')}
      >
        <input id={`${prefix}-day`} type="number" min={1} max={31} className="input" {...register('day', { valueAsNumber: true })} />
      </Field>
      <Field label={t('recurrence.validFrom')} htmlFor={`${prefix}-from`} error={errors.validFrom && t('validation.period')}>
        <input id={`${prefix}-from`} type="month" className="input" {...register('validFrom')} />
      </Field>
      {recurrence !== 'ONCE' && (
        <Field label={t('recurrence.validUntil')} htmlFor={`${prefix}-until`} error={errors.validUntil && t('validation.period')}>
          <input id={`${prefix}-until`} type="month" className="input" {...register('validUntil')} />
        </Field>
      )}
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
