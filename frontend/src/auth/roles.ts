import type { Role } from '../api/auth'

export const ROLE_LABELS: Record<Role, string> = {
  STUDENT: 'Étudiant',
  TEACHER: 'Enseignant',
  ADMIN: 'Administrateur',
}
