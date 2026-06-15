export type AvatarAsset = { uri: string; fileName?: string; mimeType?: string; fileSize?: number; width?: number; height?: number };
export type AvatarValidation = { ok: true; asset: AvatarAsset } | { ok: false; error: string };
export const AVATAR_MAX_BYTES = 5 * 1024 * 1024;
export const AVATAR_ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;

export function validateAvatarAsset(asset: AvatarAsset): AvatarValidation {
  const type = asset.mimeType?.toLowerCase();
  if (!asset.uri) return { ok: false, error: 'Choose a photo to continue.' };
  if (!type || !AVATAR_ALLOWED_TYPES.includes(type as typeof AVATAR_ALLOWED_TYPES[number])) return { ok: false, error: 'Use a JPG, PNG, or WebP avatar.' };
  if ((asset.fileSize ?? 0) > AVATAR_MAX_BYTES) return { ok: false, error: 'Avatar must be 5 MB or smaller.' };
  return { ok: true, asset };
}

export function avatarInitials(name: string): string {
  return name.split(' ').filter(Boolean).map((part) => part[0]).slice(0, 2).join('').toUpperCase() || '?';
}

export async function prepareAvatarForUpload(asset: AvatarAsset): Promise<AvatarAsset> {
  const valid = validateAvatarAsset(asset);
  if (!valid.ok) throw new Error(valid.error);
  return { ...valid.asset, width: Math.min(valid.asset.width ?? 512, 512), height: Math.min(valid.asset.height ?? 512, 512) };
}

export function simulateAvatarUpload(asset: AvatarAsset, onProgress?: (progress: number) => void, signal?: AbortSignal): Promise<string> {
  return new Promise((resolve, reject) => {
    const valid = validateAvatarAsset(asset);
    if (!valid.ok) { reject(new Error(valid.error)); return; }
    let progress = 0;
    const id = setInterval(() => {
      if (signal?.aborted) { clearInterval(id); reject(new Error('Avatar upload cancelled.')); return; }
      progress += 25;
      onProgress?.(progress);
      if (progress >= 100) { clearInterval(id); resolve(asset.uri); }
    }, 1);
  });
}
