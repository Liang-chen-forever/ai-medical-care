export function routeAccess(meta, authenticated, role) {
  if (meta.requiresAuth && !authenticated) return { name: 'Login' }
  if (meta.role && meta.role !== role) return authenticated ? { name: 'Chat' } : { name: 'Login' }
  return undefined
}
