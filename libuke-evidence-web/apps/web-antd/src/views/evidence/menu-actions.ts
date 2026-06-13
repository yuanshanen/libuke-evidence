import type { MenuRecord } from '#/api';

import { computed, ref } from 'vue';

import { getMenusApi } from '#/api';

type ButtonTitleMap = Record<string, string>;

const menuTree = ref<MenuRecord[]>([]);

function flattenMenus(items: MenuRecord[]): MenuRecord[] {
  return items.flatMap((item) => [item, ...flattenMenus(item.children ?? [])]);
}

function findMenuByPath(items: MenuRecord[], path: string): MenuRecord | undefined {
  return flattenMenus(items).find((item) => item.path === path);
}

function collectButtonTitles(menu?: MenuRecord): ButtonTitleMap {
  if (!menu) {
    return {};
  }
  return (menu.children ?? [])
    .filter((item) => item.type === 'button')
    .reduce<ButtonTitleMap>((map, item) => {
      if (item.permissionCode) {
        map[item.permissionCode] = item.title;
      }
      map[item.name] = item.title;
      return map;
    }, {});
}

export async function loadMenuActionTree() {
  try {
    menuTree.value = await getMenusApi({ includeButtons: true });
  } catch (error) {
    console.error(error);
  }
}

export function useMenuActionTitles(pagePath: string) {
  const titles = computed(() => collectButtonTitles(findMenuByPath(menuTree.value, pagePath)));

  function getActionTitle(key: string, fallback: string) {
    return titles.value[key] ?? fallback;
  }

  return {
    getActionTitle,
  };
}
