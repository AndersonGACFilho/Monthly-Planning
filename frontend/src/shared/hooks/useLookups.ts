import { useCategories } from '../../api/generated/endpoints/category-controller/category-controller';
import { useMembers } from '../../api/generated/endpoints/member-controller/member-controller';
import type { CategoryModel, MemberModel } from '../../api/generated/model';
import { embedded } from '../hal';

/**
 * Members by id, for tables that show a member code or name.
 *
 * @returns map from member id to member
 */
export function useMemberMap(): Map<number, MemberModel> {
  const { data } = useMembers({ size: 200, sort: ['code,asc'] });
  return new Map(embedded<MemberModel>(data, 'members').map((m) => [m.id ?? 0, m]));
}

/**
 * Categories by id, for tables that show a category name and colour.
 *
 * @returns map from category id to category
 */
export function useCategoryMap(): Map<number, CategoryModel> {
  const { data } = useCategories({ size: 500, sort: ['name,asc'] });
  return new Map(embedded<CategoryModel>(data, 'categories').map((c) => [c.id ?? 0, c]));
}
