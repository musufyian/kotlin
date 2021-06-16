/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#include <cerrno>
#include <string_view>
#include <cstring>
#include <cstdio>
#include <unistd.h>

#include "KAssert.h"
#include "Memory.h"
#include "Porting.h"
#include "ThreadData.hpp"
#include "ThreadRegistry.hpp"
#include "ThreadState.hpp"
#include "ExecFormat.h"

using namespace kotlin;

// this values will be substituted by compiler
extern "C" const void **Kotlin_callsCheckerKnownFunctions = nullptr;
extern "C" int Kotlin_callsCheckerKnownFunctionsCount = 0;


namespace {

constexpr const char* goodFunctionNames[] = {
        "\x01_close",
        "\x01_mprotect",

        "_ZL15_objc_terminatev", // _objc_terminate()
        "_ZNSt13exception_ptrC1ERKS_", // std::exception_ptr::exception_ptr(std::exception_ptr const&)
        "_ZNSt13exception_ptrD1Ev", // std::exception_ptr::~exception_ptr()
        "_ZNSt3__112__next_primeEm", // std::__1::__next_prime(unsigned long)
        "_ZNSt3__112basic_stringIcNS_11char_traitsIcEENS_9allocatorIcEEED1Ev", // std::__1::basic_string<char, std::__1::char_traits<char>, std::__1::allocator<char> >::~basic_string()
        "_ZNSt3__16chrono12steady_clock3nowEv", // std::__1::chrono::steady_clock::now()
        "_ZNSt3__19to_stringEi", // std::__1::to_string(int)
        "_ZNSt9exceptionD2Ev", // std::exception::~exception()
        "_ZSt17current_exceptionv", // std::current_exception()
        "_ZSt17rethrow_exceptionSt13exception_ptr", // std::rethrow_exception(std::exception_ptr)
        "_ZSt9terminatev", // std::terminate()

        "__cxa_allocate_exception",
        "__cxa_begin_catch",
        "__cxa_end_catch",
        "__cxa_throw",
        "__memset_chk",

        "abort",
        "acos",
        "acosf",
        "acosh",
        "acoshf",
        "asin",
        "asinf",
        "asinh",
        "asinhf",
        "atan",
        "atan2",
        "atan2f",
        "atanf",
        "atanh",
        "atanhf",
        "calloc",
        "clock_gettime",
        "cosh",
        "coshf",
        "exit",
        "expm1",
        "expm1f",
        "free",
        "hypot",
        "hypotf",
        "log1p",
        "log1pf",
        "malloc",
        "memcmp",
        "memmem",
        "nextafter",
        "nextafterf",
        "remainder",
        "remainderf",
        "sinh",
        "sinhf",
        "strcmp",
        "strlen",
        "tan",
        "tanf",
        "tanh",
        "tanhf",
        "vsnprintf",

        "dispatch_once",
        "\x01_pthread_cond_init",
        "_pthread_cond_init",
        "pthread_cond_destroy",
        "pthread_cond_signal",
        "pthread_equal",
        "pthread_main_np",
        "pthread_mutex_destroy",
        "pthread_mutex_init",
        "pthread_mutex_unlock",
        "pthread_self",

        "+[NSObject allocWithZone:]",
        "-[NSObject retain]",
        "+[NSError errorWithDomain:code:userInfo:]",
        "+[NSMethodSignature signatureWithObjCTypes:]",
        "+[NSNull null]",
        "+[NSObject class]",
        "+[NSObject new]",
        "+[NSString stringWithUTF8String:]",
        "+[NSValue valueWithPointer:]",
        "-[NSDictionary objectForKeyedSubscript:]",
        "-[NSError localizedDescription]",
        "-[NSError userInfo]",
        "-[NSMethodSignature getArgumentTypeAtIndex:]",
        "-[NSMethodSignature methodReturnType]",
        "-[NSMethodSignature numberOfArguments]",
        "-[NSObject init]",
        "-[NSPlaceholderString initWithBytes:length:encoding:]",
        "-[NSPlaceholderString initWithBytesNoCopy:length:encoding:freeWhenDone:]",
        "-[NSValue pointerValue]",
        "-[__NSArrayI count]",
        "-[__NSArrayI objectAtIndex:]",
        "-[__NSCFNumber doubleValue]",
        "-[__NSCFNumber floatValue]",
        "-[__NSCFNumber intValue]",
        "-[__NSCFNumber longLongValue]",
        "-[__NSCFNumber objCType]",
        "-[__NSCFString isEqual:]",
        "-[__NSDictionaryM setObject:forKeyedSubscript:]",
        "-[__NSFrozenDictionaryM objectForKeyedSubscript:]",
        "-[__SwiftNativeNSError userInfo]",
        "CFStringCreateCopy",
        "CFStringGetCharacters",
        "CFStringGetLength",
        "class_addMethod",
        "class_addProtocol",
        "class_copyMethodList",
        "class_copyProtocolList",
        "class_getClassMethod",
        "class_getName",
        "class_getSuperclass",
        "method_getName",
        "method_getTypeEncoding",
        "objc_alloc",
        "objc_allocateClassPair",
        "objc_autorelease",
        "objc_autoreleasePoolPush",
        "objc_autoreleaseReturnValue",
        "objc_getAssociatedObject",
        "objc_getClass",
        "objc_getProtocol",
        "objc_registerClassPair",
        "objc_retain",
        "objc_retainAutoreleaseReturnValue",
        "objc_retainBlock",
        "objc_setAssociatedObject",
        "object_getClass",
        "protocol_copyProtocolList",
        "protocol_getName",
        "sel_registerName",

        // @objc Swift._ContiguousArrayStorage.count.getter : Swift.Int
        "$ss23_ContiguousArrayStorageC5countSivgTo",
        // reabstraction thunk helper from @escaping @callee_guaranteed (@guaranteed __C.KtKotlinUnit?, @guaranteed Swift.Error?) -> () to @escaping @callee_unowned @convention(block) (@unowned __C.KtKotlinUnit?, @unowned __C.NSError?) -> ()
        "$sSo12KtKotlinUnitCSgs5Error_pSgIeggg_ACSo7NSErrorCSgIeyByy_TR",
        // reabstraction thunk helper from @escaping @callee_guaranteed (@guaranteed __C.KtInt?, @guaranteed Swift.Error?) -> () to @escaping @callee_unowned @convention(block) (@unowned __C.KtInt?, @unowned __C.NSError?) -> ()
        "$sSo5KtIntCSgs5Error_pSgIeggg_ACSo7NSErrorCSgIeyByy_TR",
        // @objc Swift.__SwiftDeferredNSArray.count.getter : Swift.Int
        "$ss22__SwiftDeferredNSArrayC5countSivgTo",
        // @objc Swift._ContiguousArrayStorage.objectAt(Swift.Int) -> Swift.Unmanaged<Swift.AnyObject>
        "$ss23_ContiguousArrayStorageC8objectAtys9UnmanagedVyyXlGSiFTo",
        //   @objc Swift.__SwiftNativeNSArrayWithContiguousStorage.objectAt(Swift.Int) -> Swift.Unmanaged<Swift.AnyObject>
        "$ss41__SwiftNativeNSArrayWithContiguousStorageC8objectAtys9UnmanagedVyyXlGSiFTo",
        // @objc Swift.__EmptyDictionarySingleton.count.getter : Swift.Int
        "$ss26__EmptyDictionarySingletonC5countSivgTo",
        // @objc Swift._SwiftDeferredNSDictionary.count.getter : Swift.Int
        "$ss26_SwiftDeferredNSDictionaryC5countSivgTo",
        // @objc Swift._SwiftDeferredNSDictionary.keyEnumerator() -> Swift._NSEnumerator
        "$ss26_SwiftDeferredNSDictionaryC13keyEnumerators13_NSEnumerator_pyFTo",
        // @objc Swift._SwiftDictionaryNSEnumerator.nextObject() -> Swift.AnyObject?
        "$ss28_SwiftDictionaryNSEnumeratorC10nextObjectyXlSgyFTo",
        // reabstraction thunk helper from @escaping @callee_guaranteed (@in_guaranteed Any?, @guaranteed Swift.Error?) -> () to @escaping @callee_unowned @convention(block) (@unowned Swift.AnyObject?, @unowned __C.NSError?) -> ()
        "$sypSgs5Error_pSgIegng_yXlSgSo7NSErrorCSgIeyByy_TR",

        "llvm.assume",
        "llvm.ceil.*",
        "llvm.copysign.*",
        "llvm.cos.*",
        "llvm.ctlz.*",
        "llvm.dbg.*",
        "llvm.eh.typeid.for",
        "llvm.exp.*",
        "llvm.fabs.*",
        "llvm.fabs.*",
        "llvm.floor.*",
        "llvm.lifetime.*",
        "llvm.log.*",
        "llvm.log10.*",
        "llvm.log2.*",
        "llvm.memcpy.*",
        "llvm.memmove.*",
        "llvm.memset.*",
        "llvm.objectsize.*",
        "llvm.pow.*",
        "llvm.rint.*",
        "llvm.sin.*",
        "llvm.sqrt.*",
        "llvm.umul.*",
        "llvm.va_end",
        "llvm.va_start",
        "llvm.x86.avx2.*",
        "llvm.x86.ssse3.*",

};

thread_local bool recursiveCallGuard = false;

class KnownFunctionChecker {
    std::unordered_set<const void*> known_functions;
    std::string_view good_names_copy[sizeof(goodFunctionNames) / sizeof(goodFunctionNames[0])];

public:
    bool isKnown(const void* fun) noexcept {
        if (known_functions.empty()) {
            for (int i = 0; i < Kotlin_callsCheckerKnownFunctionsCount; i++) {
                known_functions.insert(Kotlin_callsCheckerKnownFunctions[i]);
            }
        }

        return known_functions.find(fun) != known_functions.end();
    }

    bool isSafeByName(std::string_view name) noexcept {
        if (good_names_copy[0].empty()) {
            std::copy(std::begin(goodFunctionNames), std::end(goodFunctionNames), std::begin(good_names_copy));
            std::sort(std::begin(good_names_copy), std::end(good_names_copy));
        }
        auto it = std::lower_bound(std::begin(good_names_copy), std::end(good_names_copy), name);
        auto check = [&](std::string_view banned) {
            if (banned.back() != '*') {
                return banned == name;
            }
            return name.substr(0, banned.size() - 1) == banned.substr(0, banned.size() - 1);
        };
        if (it != std::end(good_names_copy) && check(*it)) {
           return true;
        }
        if (it != std::begin(good_names_copy) && check(*std::prev(it))) {
           return true;
        }
        return false;
    }

    ~KnownFunctionChecker() {
        /*
         * destroying unordered_map will call checker function, but it will not work,
         * because this object is destroying. So, let's just disable it.
         */
        recursiveCallGuard = true;
    }
};

thread_local KnownFunctionChecker checker;


constexpr int MSG_SEND_TO_NULL = -1;
constexpr int CALLED_LLVM_BUILTIN = -2;

}

/**
 * This function calls is inserted to llvm bitcode automatically, so it can be called almost anywhre.
 *
 * Although, function itself is excluded, it can call itself indirectly, from other called functions.
 * Because of this, thread_local guard is used to avoid recursive calls.
 *
 * Unfortunately, function can be called in thread constructors or destructors, where thread local data
 * should not be accessed. So before guard checking we need to check is thread destructor is running,
 * which requires special handling of recursive calls from this check.
 */
extern "C" RUNTIME_NOTHROW void Kotlin_mm_checkStateAtExternalFunctionCall(const char* caller, const char *callee, const void *calleePtr) noexcept {
    if (reinterpret_cast<int64_t>(calleePtr) == MSG_SEND_TO_NULL) return; // objc_sendMsg called on nil, it does nothing, so it's ok
    if (!strcmp(caller, "_ZN5konan36isOnThreadExitNotSetOrAlreadyStartedEv")) return;
    if (konan::isOnThreadExitNotSetOrAlreadyStarted()) return;
    if (recursiveCallGuard) return;
    if (!mm::ThreadRegistry::Instance().IsCurrentThreadRegistered()) return;
    struct unlockGuard {
        unlockGuard() { recursiveCallGuard = true; }
        ~unlockGuard() { recursiveCallGuard = false; }
    } guard;


    auto actualState = GetThreadState();
    if (actualState == ThreadState::kNative) {
        return;
    }
    if (reinterpret_cast<int64_t>(calleePtr) != CALLED_LLVM_BUILTIN && checker.isKnown(calleePtr)) {
        return;
    }

    char buf[200];
    if (callee == nullptr) {
        if (AddressToSymbol(calleePtr, buf, sizeof(buf))) {
            callee = buf;
        } else {
            callee = "unknown function";
        }
    }

    if (checker.isSafeByName(callee)) {
        return;
    }

    RuntimeFail("Expected kNative thread state at call of function %s by function %s", callee, caller);
}

