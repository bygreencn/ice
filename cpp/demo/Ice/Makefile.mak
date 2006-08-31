# **********************************************************************
#
# Copyright (c) 2003-2006 ZeroC, Inc. All rights reserved.
#
# This copy of Ice is licensed to you under the terms described in the
# ICE_LICENSE file included in this distribution.
#
# **********************************************************************

top_srcdir	= ..\..

!include $(top_srcdir)\config\Make.rules.mak

SUBDIRS		= minimal \
		  hello \
		  latency \
		  throughput \
		  value \
		  callback \
		  nested \
		  invoke \
		  bidir \
		  session \
		  converter

!if "$(BORLAND_HOME)" == ""
SUBDIRS		= $(SUBDIRS) MFC
!endif

$(EVERYTHING)::
	@for %i in ( $(SUBDIRS) ) do \
	    @echo "making $@ in %i" & \
	    cmd /c "cd %i & $(MAKE) -nologo -f Makefile.mak $@"
