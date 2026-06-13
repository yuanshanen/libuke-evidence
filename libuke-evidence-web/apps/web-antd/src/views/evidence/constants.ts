export const categoryOptions = [
  '公共卫生类',
  '公共设施类',
  '电梯问题类',
  '消防安全类',
  '秩序维护类',
].map((value) => ({ label: value, value }));

export const statusOptions = [
  { color: 'blue', label: '待处理', value: 'pending' },
  { color: 'cyan', label: '已派单', value: 'assigned' },
  { color: 'processing', label: '处理中', value: 'processing' },
  { color: 'green', label: '已处理', value: 'resolved' },
  { color: 'default', label: '已关闭', value: 'closed' },
  { color: 'red', label: '无效', value: 'invalid' },
  { color: 'warning', label: '重复', value: 'duplicate' },
];

export function statusMeta(status?: string) {
  return (
    statusOptions.find((item) => item.value === status) ?? {
      color: 'default',
      label: status || '-',
      value: status || '',
    }
  );
}
