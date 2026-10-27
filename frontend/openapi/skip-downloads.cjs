// Orval input transformer: file downloads (PDF, PNG, XLSX, CSV, HTML of a statement, workbook) are opened through
// their HAL links, not through generated hooks, and Orval cannot mock binary bodies. They are left out of the
// generated client; the contract itself (openapi.json) keeps them.
module.exports = (spec) => {
  const paths = Object.fromEntries(
    Object.entries(spec.paths ?? {}).filter(([path]) => !/\/(pdf|png|xlsx|csv|html|workbook)$/.test(path)),
  );
  return { ...spec, paths };
};
