/** Angular Imports */
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';

/** Custom Services */
import { Logger } from '../../logger/logger.service';
import { AuthenticationService } from '../authentication.service';

/** Environment Configuration */
import { environment } from '../../../../environments/environment';

/** Initialize logger */
const log = new Logger('AuthenticationGuard');

/**
 * Route access authorization.
 */
@Injectable()
export class AuthenticationGuard  {

  /**
   * @param {Router} router Router for navigation.
   * @param {AuthenticationService} authenticationService Authentication Service.
   */
  constructor(private router: Router,
              private authenticationService: AuthenticationService) { }

  /**
   * Ensures route access is authorized only when user is authenticated, otherwise redirects to login.
   *
   * @returns {boolean} True if user is authenticated.
   */
  canActivate(): boolean {
    if (environment.auth.enabled === 'false') {
      return true;
    }
    if (this.authenticationService.isAuthenticated()) {
      return true;
    }

    log.debug('User not authenticated, redirecting to login...');
    this.authenticationService.logout();
    this.router.navigate(['/login'], { replaceUrl: true });
    return false;
  }

}
