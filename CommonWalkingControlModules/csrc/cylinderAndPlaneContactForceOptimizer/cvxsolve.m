% Produced by CVXGEN, 2013-05-08 14:35:33 -0400.
% CVXGEN is Copyright (C) 2006-2012 Jacob Mattingley, jem@cvxgen.com.
% The code in this file is Copyright (C) 2006-2012 Jacob Mattingley.
% CVXGEN, or solvers produced by CVXGEN, cannot be used for commercial
% applications without prior written permission from Jacob Mattingley.

% Filename: cvxsolve.m.
% Description: Solution file, via cvx, for use with sample.m.
function [vars, status] = cvxsolve(params, settings)
C = params.C;
Qphi = params.Qphi;
Qrho = params.Qrho;
c = params.c;
phiMax = params.phiMax;
phiMin = params.phiMin;
rhoMin = params.rhoMin;
wPhi = params.wPhi;
wRho = params.wRho;
cvx_begin
  % Caution: automatically generated by cvxgen. May be incorrect.
  variable rho(40, 1);
  variable phi(10, 1);

  minimize(quad_form(Qrho*rho + Qphi*phi - c, C) + wRho*quad_form(rho, eye(40)) + wPhi*quad_form(phi, eye(10)));
  subject to
    rho >= rhoMin;
    phiMin <= phi;
    phi <= phiMax;
cvx_end
vars.phi = phi;
vars.rho = rho;
status.cvx_status = cvx_status;
% Provide a drop-in replacement for csolve.
status.optval = cvx_optval;
status.converged = strcmp(cvx_status, 'Solved');
