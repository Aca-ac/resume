export function downloadBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

/** When responseType=blob, HTTP errors still arrive as Blob — parse JSON message if possible. */
export async function assertDownloadBlob(blob: Blob): Promise<Blob> {
  const type = (blob.type || "").toLowerCase();
  const looksLikeError =
    type.includes("application/json") ||
    type.includes("text/plain") ||
    type.includes("text/html");

  if (looksLikeError) {
    const text = await blob.text();
    try {
      const json = JSON.parse(text) as { message?: string };
      throw new Error(json.message || "导出失败");
    } catch (e) {
      if (e instanceof SyntaxError) {
        throw new Error(text.slice(0, 200) || "导出失败");
      }
      throw e;
    }
  }
  if (blob.size === 0) {
    throw new Error("导出结果为空，请稍后重试");
  }
  return blob;
}
